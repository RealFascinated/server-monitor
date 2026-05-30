package cc.fascinated.monitor.metrics.vm.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;

public interface VmGaugeSeries {
    String metricName();

    default void write(MetricWriteContext ctx, double value) {
        ctx.gauge(this.metricName(), value);
    }

    default void write(MetricWriteContext ctx, long value) {
        ctx.gauge(this.metricName(), value);
    }

    default void writeNullable(MetricWriteContext ctx, Double value) {
        if (value != null) {
            write(ctx, value);
        }
    }

    default void writeNullable(MetricWriteContext ctx, Long value) {
        if (value != null) {
            write(ctx, value);
        }
    }
}
