package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.repository.AlertRuleEventStat;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

public record AlertRuleResponse(
        long id,
        long groupId,
        String groupName,
        @Nullable String name,
        String metric,
        String metricLabel,
        AlertUnit unit,
        Map<String, String> labelFilter,
        AlertOperator operator,
        double threshold,
        @Nullable Double recoveryThreshold,
        int windowSeconds,
        AlertSeverity severity,
        @Nullable Integer repeatIntervalSeconds,
        boolean enabled,
        Instant createdAt,
        long firingCount,
        long silencedCount,
        @Nullable Instant lastFiredAt
) {

    public static AlertRuleResponse from(AlertRuleRow row, String groupName, @Nullable AlertRuleEventStat stat) {
        AlertInfo info = VmMetricCatalog.alertInfoOf(row.getMetric());
        return new AlertRuleResponse(row.getId(), row.getGroupId(), groupName,
                row.getName(), row.getMetric(), info.label(), info.unit(), row.getLabelFilter(), row.getOperator(),
                row.getThreshold(), row.getRecoveryThreshold(), row.getWindowSeconds(), row.getSeverity(),
                row.getRepeatIntervalSeconds(), row.isEnabled(), row.getCreatedAt(),
                stat == null ? 0 : stat.firingCount(),
                stat == null ? 0 : stat.silencedCount(), stat == null ? null : stat.lastFiredAt());
    }
}
