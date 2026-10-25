package cc.fascinated.monitor.alert.engine;

/**
 * The alert state machine, with no Spring or I/O so the live ingest sink and the preview endpoint
 * run exactly the same logic. States are OK and FIRING; the averaging window replaces a separate pending state.
 */
public final class AlertRuleEngine {

    public enum Change {
        NONE,
        FIRED,
        /** A silenced series got worse than when it was silenced, so it is no longer silenced. */
        ESCALATED,
        RESOLVED
    }

    /** {@code wasSilenced} is only set on {@link Change#RESOLVED}: the alert was muted, so its end is not news. */
    public record Evaluation(Change change, double effectiveValue, boolean wasSilenced) {
        Evaluation(Change change, double effectiveValue) {
            this(change, effectiveValue, false);
        }
    }

    private AlertRuleEngine() {
    }

    public static Evaluation evaluate(RuleSpec rule, SeriesState state, long timestampSeconds, double value) {
        synchronized (state) {
            if (!state.add(timestampSeconds, value)) {
                return new Evaluation(Change.NONE, value);
            }
            double effective = state.average(value);
            if (!state.covered()) {
                return new Evaluation(Change.NONE, effective);
            }
            if (!state.isFiringInternal()) {
                if (rule.operator().test(effective, rule.threshold())) {
                    state.fire(timestampSeconds, effective);
                    return new Evaluation(Change.FIRED, effective);
                }
                return new Evaluation(Change.NONE, effective);
            }
            boolean escalated = state.escalates(rule.operator(), effective);
            if (rule.operator().isWorse(effective, state.worstValue())) {
                state.updateWorst(effective);
            }
            if (!rule.operator().test(effective, rule.effectiveRecoveryThreshold())) {
                boolean wasSilenced = state.isSilenced();
                state.resolve();
                return new Evaluation(Change.RESOLVED, effective, wasSilenced);
            }
            if (escalated) {
                state.unsilence();
                return new Evaluation(Change.ESCALATED, effective);
            }
            return new Evaluation(Change.NONE, effective);
        }
    }
}
