package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.shared.support.CounterAccumulator;
import cc.fascinated.monitor.metrics.shared.support.HistogramAccumulator;
import cc.fascinated.monitor.metrics.shared.support.LabeledCounterAccumulator;
import cc.fascinated.monitor.metrics.shared.support.LabeledHistogramAccumulator;
import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlatformMetricsRecorder {
    private final MonitorMetricsProperties monitorMetricsProperties;
    private final CounterAccumulator ingests = new CounterAccumulator();
    private final CounterAccumulator ingestAuthFailures = new CounterAccumulator();
    private final CounterAccumulator ingestBackfilledSamples = new CounterAccumulator();
    private final LabeledCounterAccumulator ingestRejectedSamples = new LabeledCounterAccumulator();
    private final HistogramAccumulator ingestDuration = new HistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);
    private final HistogramAccumulator ingestPayloadBytes = new HistogramAccumulator(HistogramAccumulator.PAYLOAD_BUCKETS);

    private final CounterAccumulator vmQueries = new CounterAccumulator();
    private final CounterAccumulator vmQueryErrors = new CounterAccumulator();
    private final HistogramAccumulator vmQueryDuration = new HistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);

    private final CounterAccumulator vmWrites = new CounterAccumulator();
    private final CounterAccumulator vmWriteErrors = new CounterAccumulator();
    private final HistogramAccumulator vmWriteDuration = new HistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);

    private final LabeledCounterAccumulator httpRequests = new LabeledCounterAccumulator();
    private final LabeledHistogramAccumulator httpRequestDuration = new LabeledHistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);

    private final CounterAccumulator alertEvaluations = new CounterAccumulator();
    private final LabeledCounterAccumulator alertTransitions = new LabeledCounterAccumulator();
    private final LabeledCounterAccumulator alertNotifications = new LabeledCounterAccumulator();
    private final CounterAccumulator alertDropped = new CounterAccumulator();

    public void recordAlertEvaluation() {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.alertEvaluations.increment();
    }

    public void recordAlertTransition(String type) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.alertTransitions.increment(type);
    }

    public void recordAlertNotification(String status) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.alertNotifications.increment(status);
    }

    public void recordAlertDropped() {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.alertDropped.increment();
    }

    public void recordIngest(double durationSeconds, long payloadBytes) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.ingests.increment();
        this.ingestDuration.observe(durationSeconds);
        if (payloadBytes > 0) {
            this.ingestPayloadBytes.observe(payloadBytes);
        }
    }

    public void recordIngestAuthFailure() {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.ingestAuthFailures.increment();
    }

    public void recordBackfilledSamples(int accepted) {
        if (!this.monitorMetricsProperties.isEnabled() || accepted <= 0) {
            return;
        }
        this.ingestBackfilledSamples.add(accepted);
    }

    public void recordRejectedSamples(String reason, int count) {
        if (!this.monitorMetricsProperties.isEnabled() || count <= 0) {
            return;
        }
        this.ingestRejectedSamples.add(reason, count);
    }

    public void recordVmQuery(double durationSeconds, boolean success) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.vmQueries.increment();
        this.vmQueryDuration.observe(durationSeconds);
        if (!success) {
            this.vmQueryErrors.increment();
        }
    }

    public void recordVmWrite(double durationSeconds, boolean success) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.vmWrites.increment();
        this.vmWriteDuration.observe(durationSeconds);
        if (!success) {
            this.vmWriteErrors.increment();
        }
    }

    public void recordHttpRequest(String method, String pathPattern, int status, double durationSeconds) {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        String counterKey = httpLabelKey(method, pathPattern, String.valueOf(status));
        this.httpRequests.increment(counterKey);
        this.httpRequestDuration.observe(httpLabelKey(method, pathPattern), durationSeconds);
    }

    public void writeCountersAndHistograms(PrometheusWriteContext ctx) {
        writeIngest(ctx);
        writeVm(ctx);
        writeHttp(ctx);
        writeAlerts(ctx);
    }

    private void writeAlerts(PrometheusWriteContext ctx) {
        ctx.counter(PlatformMetricFamily.ALERT_EVALUATIONS_TOTAL.metricName(), this.alertEvaluations.get());
        ctx.counter(PlatformMetricFamily.ALERT_DROPPED_TOTAL.metricName(), this.alertDropped.get());
        for (Map.Entry<String, Long> entry : this.alertTransitions.snapshot().entrySet()) {
            ctx.counter(PlatformMetricFamily.ALERT_TRANSITIONS_TOTAL.metricName(), entry.getValue(),
                    Map.of("type", entry.getKey()));
        }
        for (Map.Entry<String, Long> entry : this.alertNotifications.snapshot().entrySet()) {
            ctx.counter(PlatformMetricFamily.ALERT_NOTIFICATIONS_TOTAL.metricName(), entry.getValue(),
                    Map.of("status", entry.getKey()));
        }
    }

    private void writeIngest(PrometheusWriteContext ctx) {
        ctx.counter(PlatformMetricFamily.INGESTS_TOTAL.metricName(), this.ingests.get());
        ctx.counter(PlatformMetricFamily.INGEST_AUTH_FAILURES_TOTAL.metricName(), this.ingestAuthFailures.get());
        ctx.counter(PlatformMetricFamily.INGEST_BACKFILLED_SAMPLES_TOTAL.metricName(), this.ingestBackfilledSamples.get());
        for (Map.Entry<String, Long> entry : this.ingestRejectedSamples.snapshot().entrySet()) {
            ctx.counter(PlatformMetricFamily.INGEST_REJECTED_SAMPLES_TOTAL.metricName(), entry.getValue(),
                    Map.of("reason", entry.getKey()));
        }
        ctx.histogram(PlatformMetricFamily.INGEST_DURATION_SECONDS.metricName(), this.ingestDuration, Map.of());
        ctx.histogram(PlatformMetricFamily.INGEST_PAYLOAD_BYTES.metricName(), this.ingestPayloadBytes, Map.of());
    }

    private void writeVm(PrometheusWriteContext ctx) {
        ctx.counter(PlatformMetricFamily.VM_QUERIES_TOTAL.metricName(), this.vmQueries.get());
        ctx.counter(PlatformMetricFamily.VM_QUERY_ERRORS_TOTAL.metricName(), this.vmQueryErrors.get());
        ctx.histogram(PlatformMetricFamily.VM_QUERY_DURATION_SECONDS.metricName(), this.vmQueryDuration, Map.of());
        ctx.counter(PlatformMetricFamily.VM_WRITES_TOTAL.metricName(), this.vmWrites.get());
        ctx.counter(PlatformMetricFamily.VM_WRITE_ERRORS_TOTAL.metricName(), this.vmWriteErrors.get());
        ctx.histogram(PlatformMetricFamily.VM_WRITE_DURATION_SECONDS.metricName(), this.vmWriteDuration, Map.of());
    }

    private void writeHttp(PrometheusWriteContext ctx) {
        for (Map.Entry<String, Long> entry : this.httpRequests.snapshot().entrySet()) {
            String[] parts = entry.getKey().split("\0", 3);
            Map<String, String> labels = Map.of("method", parts[0], "path", parts[1], "status", parts[2]);
            ctx.counter(PlatformMetricFamily.HTTP_REQUESTS_TOTAL.metricName(), entry.getValue(), labels);
        }
        for (Map.Entry<String, HistogramAccumulator> entry : this.httpRequestDuration.snapshot().entrySet()) {
            String[] parts = entry.getKey().split("\0", 2);
            Map<String, String> labels = Map.of("method", parts[0], "path", parts[1]);
            ctx.histogram(PlatformMetricFamily.HTTP_REQUEST_DURATION_SECONDS.metricName(), entry.getValue(), labels);
        }
    }

    private static String httpLabelKey(String method, String pathPattern) {
        return method + "\0" + pathPattern;
    }

    private static String httpLabelKey(String method, String pathPattern, String status) {
        return method + "\0" + pathPattern + "\0" + status;
    }
}
