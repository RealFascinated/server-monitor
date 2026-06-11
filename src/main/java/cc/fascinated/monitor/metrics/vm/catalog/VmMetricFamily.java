package cc.fascinated.monitor.metrics.vm.catalog;

import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;

public interface VmMetricFamily extends VmGaugeSeries {
    MetricFamily metricFamily();

    String metricPrefix();
}
