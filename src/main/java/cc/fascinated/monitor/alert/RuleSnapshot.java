package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.RuleSpec;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Immutable view of a rule, safe to share with the ingest threads. */
public record RuleSnapshot(
        long id,
        long userId,
        @Nullable String name,
        String metric,
        Map<String, String> labelFilter,
        RuleSpec spec,
        AlertSeverity severity,
        @Nullable Integer repeatIntervalSeconds,
        @Nullable Long destinationId
) {

    /** {@code destinationId} is the destination of the rule's group. */
    public static RuleSnapshot from(AlertRuleRow row, @Nullable Long destinationId) {
        return new RuleSnapshot(
                row.getId(),
                row.getUserId(),
                row.getName(),
                row.getMetric(),
                row.getLabelFilter() == null ? Map.of() : Map.copyOf(row.getLabelFilter()),
                new RuleSpec(row.getOperator(), row.getThreshold(), row.getRecoveryThreshold(), row.getWindowSeconds()),
                row.getSeverity(),
                row.getRepeatIntervalSeconds(),
                destinationId
        );
    }

    public boolean matches(Map<String, String> labels) {
        for (Map.Entry<String, String> filter : this.labelFilter.entrySet()) {
            if (!filter.getValue().equals(labels.get(filter.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
