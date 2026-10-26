package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.AgentMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Agent process self-metrics. Stored per server but only served through the admin fleet metrics,
 * so they are deliberately not part of the per-server metric catalog.
 */
@Getter
@Accessors(fluent = true)
public enum AgentSeries implements VmGaugeSeries {
    RSS_BYTES("monitor_agent_rss_bytes"),
    GOROUTINES("monitor_agent_goroutines"),
    HEAP_IN_USE_BYTES("monitor_agent_heap_in_use_bytes"),
    OPEN_FDS("monitor_agent_open_fds"),
    UPTIME_SECONDS("monitor_agent_uptime_seconds"),
    LAST_PUSH_DURATION_MS("monitor_agent_last_push_duration_ms"),
    LAST_PUSH_PAYLOAD_BYTES("monitor_agent_last_push_payload_bytes"),
    PUSH_FAILURES_TOTAL("monitor_agent_push_failures_total"),
    BUFFERED_SAMPLES("monitor_agent_buffered_samples"),
    BUFFERED_BYTES("monitor_agent_buffered_bytes"),
    BUFFER_OLDEST_AGE_SECONDS("monitor_agent_buffer_oldest_age_seconds"),
    BUFFER_DROPPED_TOTAL("monitor_agent_buffer_dropped_total");

    public static final String METRIC_NAME_PREFIX = "monitor_agent_";

    private final String metricName;

    AgentSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void write(MetricWriteContext ctx, AgentMetrics agent) {
        RSS_BYTES.writeNullable(ctx, agent.rssBytes());
        GOROUTINES.writeNullable(ctx, agent.goroutines());
        HEAP_IN_USE_BYTES.writeNullable(ctx, agent.heapInUseBytes());
        OPEN_FDS.writeNullable(ctx, agent.openFds());
        UPTIME_SECONDS.writeNullable(ctx, agent.uptimeSeconds());
        LAST_PUSH_DURATION_MS.writeNullable(ctx, agent.lastPushDurationMs());
        LAST_PUSH_PAYLOAD_BYTES.writeNullable(ctx, agent.lastPushPayloadBytes());
        PUSH_FAILURES_TOTAL.writeNullable(ctx, agent.pushFailuresTotal());
        BUFFERED_SAMPLES.writeNullable(ctx, agent.bufferedSamples());
        BUFFERED_BYTES.writeNullable(ctx, agent.bufferedBytes());
        BUFFER_OLDEST_AGE_SECONDS.writeNullable(ctx, agent.bufferOldestAgeSeconds());
        BUFFER_DROPPED_TOTAL.writeNullable(ctx, agent.bufferDroppedTotal());
    }
}
