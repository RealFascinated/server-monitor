package cc.fascinated.monitor.model.domain.alert;

public enum AlertTransition {
    FIRING,
    REPEAT,
    /** A silenced alert got worse than it was when silenced, so it speaks up again. */
    ESCALATED,
    RESOLVED
}
