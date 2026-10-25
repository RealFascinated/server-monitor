package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.AlertRuleEngine;
import cc.fascinated.monitor.alert.engine.RuleSpec;
import cc.fascinated.monitor.alert.engine.SeriesState;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmSample;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.dto.request.alert.AlertPreviewRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertPreviewResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Replays a draft rule over a server's recent history using the same engine live evaluation uses, so the
 * builder shows what the rule would really have done. History comes at query-step resolution.
 */
@Service
public class AlertPreviewService {
    private static final int MAX_SERIES = 10;
    private static final int TARGET_POINTS = 480;
    private static final int MIN_STEP_SECONDS = 15;

    private final VictoriaMetricsQueryClient queryClient;
    private final ServerService serverService;
    private final AlertRuleValidator validator;

    public AlertPreviewService(VictoriaMetricsQueryClient queryClient, ServerService serverService,
                               AlertRuleValidator validator) {
        this.queryClient = queryClient;
        this.serverService = serverService;
        this.validator = validator;
    }

    public AlertPreviewResponse preview(UserRow user, AlertPreviewRequest request) {
        this.serverService.requireServer(user, request.serverId(), ServerPermission.VIEW_METRICS);
        AlertRuleValidator.Definition definition = this.validator.validate(request.metric(), request.labelFilter(),
                request.operator(), request.threshold(), request.recoveryThreshold(), request.windowSeconds());
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(request.rangeHours()));
        int stepSeconds = (int) Math.max(MIN_STEP_SECONDS, Duration.between(from, to).getSeconds() / TARGET_POINTS);

        VictoriaMetricsQuery.Builder query = VictoriaMetricsQuery.builder()
                .metric(definition.metric())
                .serverId(request.serverId())
                .from(from).to(to).step(Duration.ofSeconds(stepSeconds));
        definition.labelFilter().forEach(query::label);

        List<VmTimeSeries> all = new ArrayList<>(this.queryClient.execute(query.build()).timeSeries());
        all.sort(Comparator.comparing(series -> SeriesKeys.keyOf(SeriesKeys.seriesLabels(series.labels()))));
        boolean truncated = all.size() > MAX_SERIES;

        List<AlertPreviewResponse.PreviewSeries> result = new ArrayList<>();
        int firingCount = 0;
        for (VmTimeSeries series : all.subList(0, Math.min(MAX_SERIES, all.size()))) {
            AlertPreviewResponse.PreviewSeries replayed = replay(series, definition.spec());
            firingCount += replayed.firing().size();
            result.add(replayed);
        }
        AlertUnit unit = VmMetricCatalog.alertInfoOf(definition.metric()).unit();
        return new AlertPreviewResponse(unit, from.getEpochSecond(), to.getEpochSecond(), stepSeconds, result, firingCount, truncated);
    }

    static AlertPreviewResponse.PreviewSeries replay(VmTimeSeries series, RuleSpec spec) {
        SeriesState state = new SeriesState(spec.windowSeconds());
        List<Long> timestamps = new ArrayList<>(series.samples().size());
        List<Double> values = new ArrayList<>(series.samples().size());
        List<AlertPreviewResponse.Interval> firing = new ArrayList<>();
        long startedAt = 0;
        double peak = 0;
        long lastTs = 0;
        for (VmSample sample : series.samples()) {
            long ts = sample.timestamp().getEpochSecond();
            lastTs = ts;
            timestamps.add(ts);
            values.add(sample.value());
            AlertRuleEngine.Evaluation evaluation = AlertRuleEngine.evaluate(spec, state, ts, sample.value());
            switch (evaluation.change()) {
                case FIRED -> startedAt = ts;
                case RESOLVED -> firing.add(new AlertPreviewResponse.Interval(startedAt, ts, false, state.worstValue()));
                case NONE, ESCALATED -> {
                }
            }
            peak = state.worstValue();
        }
        if (state.isFiring()) {
            firing.add(new AlertPreviewResponse.Interval(startedAt, lastTs, true, peak));
        }
        return new AlertPreviewResponse.PreviewSeries(SeriesKeys.seriesLabels(series.labels()), timestamps, values, firing);
    }
}
