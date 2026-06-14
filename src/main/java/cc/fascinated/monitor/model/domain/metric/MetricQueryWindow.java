package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;

import java.time.Duration;
import java.time.Instant;

public record MetricQueryWindow(Instant from, Instant to, Duration step) {
    public static MetricQueryWindow parse(long fromEpoch, long toEpoch) {
        Instant from = Instant.ofEpochSecond(fromEpoch);
        Instant to = Instant.ofEpochSecond(toEpoch);
        Instant now = Instant.now();
        Instant clampedTo = to.isAfter(now) ? now : to;
        if (!from.isBefore(clampedTo)) {
            throw new BadRequestException("from must be before to");
        }
        Duration span = Duration.between(from, clampedTo);
        if (span.compareTo(MetricStepPolicy.minSpan()) < 0) {
            throw new BadRequestException("Metric window must be at least 5 minutes");
        }
        if (span.compareTo(MetricStepPolicy.maxSpan()) > 0) {
            throw new BadRequestException("Metric window must be at most 2 years");
        }
        return new MetricQueryWindow(from, clampedTo, MetricStepPolicy.stepFor(span));
    }

    public long stepSeconds() {
        return this.step.getSeconds();
    }

    public Duration span() {
        return Duration.between(this.from, this.to);
    }

    public long fromEpoch() {
        return this.from.getEpochSecond();
    }

    public long toEpoch() {
        return this.to.getEpochSecond();
    }

    public VictoriaMetricsQuery toRangeQuery(String promql) {
        return VictoriaMetricsQuery.builder()
                .query(promql)
                .from(this.from)
                .to(this.to)
                .step(this.step)
                .build();
    }
}
