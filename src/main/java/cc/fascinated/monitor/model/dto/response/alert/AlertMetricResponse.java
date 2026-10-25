package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;

import java.util.List;

public record AlertMetricResponse(
        String metric,
        String label,
        AlertUnit unit,
        AlertOperator defaultOperator,
        double defaultThreshold,
        String group,
        List<String> labelKeys
) {

    public static AlertMetricResponse from(VmGaugeSeries series, AlertInfo info, MetricFamily family) {
        return new AlertMetricResponse(series.metricName(), info.label(), info.unit(), info.operator(), info.threshold(),
                family == null ? "host" : family.section().jsonKey(),
                family == null ? List.of() : List.of(family.identityLabels()));
    }
}
