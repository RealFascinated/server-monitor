package cc.fascinated.monitor.alert.engine;

import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import org.jetbrains.annotations.Nullable;

/** The part of a rule the state machine needs. */
public record RuleSpec(AlertOperator operator, double threshold, @Nullable Double recoveryThreshold, int windowSeconds) {

    public double effectiveRecoveryThreshold() {
        return this.recoveryThreshold != null ? this.recoveryThreshold : this.threshold;
    }
}
