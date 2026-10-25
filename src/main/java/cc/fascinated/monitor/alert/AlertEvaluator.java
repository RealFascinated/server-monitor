package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.AlertRuleEngine;
import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.config.MonitorProperties;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import cc.fascinated.monitor.service.UserPreferencesService;
import cc.fascinated.monitor.metrics.vm.MetricSink;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.persistance.AlertEventRow;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates rules against live samples as they are ingested. Everything here is in memory; transitions are
 * handed to the {@link AlertDispatcher}, so the ingest thread never does I/O for alerting.
 */
@Slf4j
@Component
public class AlertEvaluator {
    private final AlertRuleCache ruleCache;
    private final AlertDispatcher dispatcher;
    private final MonitorAlertProperties properties;
    private final MonitorProperties monitorProperties;
    private final PlatformMetricsRecorder recorder;
    private final ConcurrentHashMap<StateKey, TrackedSeries> states = new ConcurrentHashMap<>();

    record StateKey(long ruleId, long serverId, String seriesKey) {}

    public AlertEvaluator(AlertRuleCache ruleCache, AlertDispatcher dispatcher, MonitorAlertProperties properties,
                          MonitorProperties monitorProperties, PlatformMetricsRecorder recorder) {
        this.ruleCache = ruleCache;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.monitorProperties = monitorProperties;
        this.recorder = recorder;
    }

    /** Returns a sink for live ingest of this server, or null when no rule applies to it (zero cost). */
    @Nullable
    public MetricSink sinkFor(long serverId, long timestampSeconds) {
        ServerRules rules = this.ruleCache.get(serverId);
        if (rules.isEmpty()) {
            return null;
        }
        return (metric, value, labels) -> {
            try {
                observe(rules, timestampSeconds, metric, value, labels);
            } catch (RuntimeException ex) {
                // alerting must never break metric ingest
                log.error("Alert evaluation failed for {}", metric, ex);
            }
        };
    }

    /** Feeds a sample for a server without going through ingest, used for the offline sweep. */
    public void observeServerUp(long serverId, long timestampSeconds, double value) {
        ServerRules rules = this.ruleCache.get(serverId);
        if (rules.isEmpty()) {
            return;
        }
        observe(rules, timestampSeconds, ServerStatusSeries.UP.metricName(), value, Map.of("server_id", Long.toString(serverId)));
    }

    public boolean hasServerUpRules(long serverId) {
        return this.ruleCache.get(serverId).byMetric().containsKey(ServerStatusSeries.UP.metricName());
    }

    /**
     * Housekeeping, run from the sweeper: closes series that stopped reporting, drops idle state and
     * emits repeat reminders for alerts that are still firing.
     */
    public void sweep(long nowMillis) {
        long ttlMillis = this.properties.getStateTtl().toMillis();
        Instant now = Instant.ofEpochMilli(nowMillis);
        for (Map.Entry<StateKey, TrackedSeries> entry : this.states.entrySet()) {
            StateKey key = entry.getKey();
            TrackedSeries tracked = entry.getValue();
            long lastSeenMillis = tracked.state.lastSeen() * 1000;
            boolean stale = nowMillis - lastSeenMillis > ttlMillis;
            if (stale) {
                if (this.states.remove(key, tracked) && tracked.state.isFiring()) {
                    this.dispatcher.submit(new AlertSignal(AlertTransition.RESOLVED, key.ruleId(), key.serverId(),
                            key.seriesKey(), null, tracked.state.worstValue(), now,
                            AlertEventRow.REASON_SERIES_GONE, null));
                }
                continue;
            }
            if (tracked.state.isFiring() && !tracked.state.isSilenced()) {
                remind(key, tracked, nowMillis, now);
            }
        }
    }

    /** Drops all state for rules whose definition changed; their open events are closed by the caller. */
    public void purgeRules(java.util.Collection<Long> ruleIds) {
        if (ruleIds.isEmpty()) {
            return;
        }
        this.states.keySet().removeIf(key -> ruleIds.contains(key.ruleId()));
    }

    /** Drops the state of some rules on one server, e.g. when it leaves their group. */
    public void purgeServerRules(java.util.Collection<Long> ruleIds, long serverId) {
        if (ruleIds.isEmpty()) {
            return;
        }
        this.states.keySet().removeIf(key -> key.serverId() == serverId && ruleIds.contains(key.ruleId()));
    }

    /** Forgets all in-memory state; used by tests. */
    public void purgeAll() {
        this.states.clear();
    }

    /**
     * Mutes a firing series until it gets worse than it is now. Returns that baseline, or null when nothing
     * in memory is firing for the key (the caller then falls back to the event's recorded peak).
     */
    @Nullable
    public Double silence(long ruleId, long serverId, String seriesKey) {
        TrackedSeries tracked = this.states.get(new StateKey(ruleId, serverId, seriesKey));
        if (tracked == null) {
            return null;
        }
        double baseline = tracked.state.worstValue();
        return tracked.state.silence(baseline) ? baseline : null;
    }

    public void unsilence(long ruleId, long serverId, String seriesKey) {
        TrackedSeries tracked = this.states.get(new StateKey(ruleId, serverId, seriesKey));
        if (tracked != null) {
            tracked.state.unsilence();
        }
    }

    /** Restores a firing series after a restart so it does not notify again. */
    void restore(long ruleId, long serverId, String seriesKey, String metric, int windowSeconds, Instant startedAt,
                 double peak, @Nullable Instant lastNotifiedAt, @Nullable Double silencedValue) {
        TrackedSeries tracked = new TrackedSeries(windowSeconds, metric, SeriesKeys.labelsOf(seriesKey));
        long nowSeconds = Instant.now().getEpochSecond();
        tracked.state.restoreFiring(startedAt.getEpochSecond(), peak, nowSeconds, silencedValue);
        tracked.lastNotifiedMillis = lastNotifiedAt != null ? lastNotifiedAt.toEpochMilli() : startedAt.toEpochMilli();
        this.states.put(new StateKey(ruleId, serverId, seriesKey), tracked);
    }

    private void observe(ServerRules serverRules, long ts, String metric, double value, Map<String, String> labels) {
        RuleSnapshot[] rules = serverRules.byMetric().get(metric);
        if (rules == null || !Double.isFinite(value)) {
            return;
        }
        Map<String, String> seriesLabels = null;
        String seriesKey = null;
        for (RuleSnapshot rule : rules) {
            if (!rule.matches(labels)) {
                continue;
            }
            if (seriesKey == null) {
                seriesLabels = SeriesKeys.seriesLabels(labels);
                seriesKey = SeriesKeys.keyOf(seriesLabels);
            }
            StateKey key = new StateKey(rule.id(), serverRules.serverId(), seriesKey);
            TrackedSeries tracked = this.states.get(key);
            if (tracked == null || tracked.windowSeconds != rule.spec().windowSeconds()) {
                TrackedSeries fresh = new TrackedSeries(rule.spec().windowSeconds(), metric, seriesLabels);
                if (tracked == null) {
                    tracked = this.states.computeIfAbsent(key, ignored -> fresh);
                } else {
                    this.states.put(key, fresh);
                    tracked = fresh;
                }
            }
            this.recorder.recordAlertEvaluation();
            AlertRuleEngine.Evaluation evaluation = AlertRuleEngine.evaluate(rule.spec(), tracked.state, ts, value);
            switch (evaluation.change()) {
                case FIRED -> {
                    tracked.lastNotifiedMillis = System.currentTimeMillis();
                    emit(serverRules, rule, key, tracked, AlertTransition.FIRING, evaluation.effectiveValue(),
                            tracked.state.worstValue(), null, Instant.ofEpochSecond(ts));
                }
                case ESCALATED -> {
                    tracked.lastNotifiedMillis = System.currentTimeMillis();
                    emit(serverRules, rule, key, tracked, AlertTransition.ESCALATED, evaluation.effectiveValue(),
                            tracked.state.worstValue(), null, Instant.ofEpochSecond(ts));
                }
                case RESOLVED -> {
                    if (evaluation.wasSilenced()) {
                        // a muted alert ends quietly: close the event without telling anyone
                        this.dispatcher.submit(new AlertSignal(AlertTransition.RESOLVED, rule.id(), key.serverId(),
                                key.seriesKey(), rule.destinationId(), tracked.state.worstValue(),
                                Instant.ofEpochSecond(ts), AlertEventRow.REASON_RECOVERED, null));
                    } else {
                        emit(serverRules, rule, key, tracked, AlertTransition.RESOLVED, evaluation.effectiveValue(),
                                tracked.state.worstValue(), AlertEventRow.REASON_RECOVERED, Instant.ofEpochSecond(ts));
                    }
                }
                case NONE -> {
                }
            }
        }
    }

    private void emit(ServerRules serverRules, RuleSnapshot rule, StateKey key, TrackedSeries tracked,
                      AlertTransition transition, double value, double peak, @Nullable String resolveReason,
                      Instant at) {
        Duration firedFor = transition == AlertTransition.RESOLVED
                ? Duration.between(Instant.ofEpochSecond(tracked.state.firingSince()), at)
                : null;
        this.recorder.recordAlertTransition(transition.name());
        this.dispatcher.submit(new AlertSignal(transition, rule.id(), key.serverId(), key.seriesKey(),
                rule.destinationId(), peak, at, resolveReason,
                notification(serverRules, rule, tracked, transition, value, firedFor, at)));
    }

    private AlertNotification notification(ServerRules serverRules, RuleSnapshot rule, TrackedSeries tracked,
                                           AlertTransition transition, double value, @Nullable Duration firedFor,
                                           Instant at) {
        AlertInfo info = VmMetricCatalog.alertInfoOf(rule.metric());
        TemperatureUnit temperatureUnit = TemperatureUnit.fromKey(
                (String) UserPreferencesService.Preferences.TEMPERATURE_UNIT.getValue(rule.userId()));
        return new AlertNotification(rule.id(), rule.name(), serverRules.serverId(), serverRules.serverName(),
                serverUrl(serverRules.serverId()), rule.metric(), info.label(), tracked.labels,
                rule.spec().operator(), rule.spec().threshold(), value, info.unit(), temperatureUnit, rule.severity(), transition,
                rule.spec().windowSeconds(), firedFor, at);
    }

    private String serverUrl(long serverId) {
        String base = this.monitorProperties.getWebsiteUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/servers/" + serverId;
    }

    private void remind(StateKey key, TrackedSeries tracked, long nowMillis, Instant now) {
        ServerRules serverRules = this.ruleCache.get(key.serverId());
        RuleSnapshot[] rules = serverRules.byMetric().get(tracked.metric);
        if (rules == null) {
            return;
        }
        for (RuleSnapshot rule : rules) {
            if (rule.id() != key.ruleId() || rule.repeatIntervalSeconds() == null) {
                continue;
            }
            if (nowMillis - tracked.lastNotifiedMillis < rule.repeatIntervalSeconds() * 1000L) {
                return;
            }
            tracked.lastNotifiedMillis = nowMillis;
            Duration firedFor = Duration.between(Instant.ofEpochSecond(tracked.state.firingSince()), now);
            this.recorder.recordAlertTransition(AlertTransition.REPEAT.name());
            this.dispatcher.submit(new AlertSignal(AlertTransition.REPEAT, rule.id(), key.serverId(), key.seriesKey(),
                    rule.destinationId(), tracked.state.worstValue(), now, null,
                    notification(serverRules, rule, tracked, AlertTransition.REPEAT, tracked.state.worstValue(),
                            firedFor, now)));
            return;
        }
    }
}
