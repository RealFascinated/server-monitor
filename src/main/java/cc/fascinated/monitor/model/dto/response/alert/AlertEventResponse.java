package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.alert.SeriesKeys;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

public record AlertEventResponse(
        long id,
        long ruleId,
        @Nullable String ruleName,
        String metric,
        String metricLabel,
        AlertUnit unit,
        AlertOperator operator,
        double threshold,
        long serverId,
        @Nullable String serverName,
        Map<String, String> labels,
        Instant startedAt,
        @Nullable Instant resolvedAt,
        @Nullable String resolveReason,
        double peakValue,
        @Nullable Instant lastNotifiedAt,
        @Nullable String notifyError,
        @Nullable Instant silencedAt,
        @Nullable Double silencedValue
) {

    public static AlertEventResponse from(AlertEventRow event, AlertRuleRow rule, @Nullable String serverName) {
        AlertInfo info = VmMetricCatalog.alertInfoOf(rule.getMetric());
        return new AlertEventResponse(event.getId(), event.getRuleId(), rule.getName(), rule.getMetric(), info.label(),
                info.unit(), rule.getOperator(), rule.getThreshold(), event.getServerId(), serverName,
                SeriesKeys.labelsOf(event.getSeriesKey()), event.getStartedAt(), event.getResolvedAt(),
                event.getResolveReason(), event.getPeakValue(), event.getLastNotifiedAt(), event.getNotifyError(),
                event.getSilencedAt(), event.getSilencedValue());
    }
}
