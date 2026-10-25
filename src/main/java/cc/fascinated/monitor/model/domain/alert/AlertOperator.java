package cc.fascinated.monitor.model.domain.alert;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum AlertOperator {
    LESS_THAN("below", "back above", (value, threshold) -> value < threshold),
    MORE_THAN("above", "back below", (value, threshold) -> value > threshold);

    private final String phrase;
    private final String recoveryPhrase;
    private final Comparison comparison;

    AlertOperator(String phrase, String recoveryPhrase, Comparison comparison) {
        this.phrase = phrase;
        this.recoveryPhrase = recoveryPhrase;
        this.comparison = comparison;
    }

    public boolean test(double value, double threshold) {
        return this.comparison.test(value, threshold);
    }

    /** True when {@code candidate} is further past the threshold than {@code current}. */
    public boolean isWorse(double candidate, double current) {
        return this == MORE_THAN ? candidate > current : candidate < current;
    }

    @FunctionalInterface
    private interface Comparison {
        boolean test(double value, double threshold);
    }
}
