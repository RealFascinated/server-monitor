package cc.fascinated.monitor.model.domain.alert;

import org.jetbrains.annotations.Nullable;

/**
 * Alerting metadata a gauge series carries: its friendly name, unit and the condition the rule builder
 * suggests. A series with no AlertInfo cannot be alerted on. {@code defaultRule} marks the series that get
 * an account-wide rule when a user is created.
 */
public record AlertInfo(
        String label,
        AlertUnit unit,
        AlertOperator operator,
        double threshold,
        @Nullable DefaultRule defaultRule
) {

    public record DefaultRule(int windowSeconds, AlertSeverity severity) {}

    public static AlertInfo of(String label, AlertUnit unit, AlertOperator operator, double threshold) {
        return new AlertInfo(label, unit, operator, threshold, null);
    }

    public AlertInfo asDefault(int windowSeconds, AlertSeverity severity) {
        return new AlertInfo(this.label, this.unit, this.operator, this.threshold, new DefaultRule(windowSeconds, severity));
    }
}
