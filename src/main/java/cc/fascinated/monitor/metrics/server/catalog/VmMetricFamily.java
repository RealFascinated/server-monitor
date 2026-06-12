package cc.fascinated.monitor.metrics.server.catalog;

import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;

public interface VmMetricFamily extends VmGaugeSeries {
    MetricFamily metricFamily();

    String metricPrefix();
}
