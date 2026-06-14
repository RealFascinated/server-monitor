package cc.fascinated.monitor.model.domain.metric;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class MetricStepPolicy {
    private static final int MAX_POINTS = 400;
    private static final Duration MIN_STEP = Duration.ofSeconds(15);
    private static final Duration MIN_SPAN = Duration.ofMinutes(5);
    private static final Duration MAX_SPAN = Duration.ofDays(730);
    private static final long[] NICE_STEP_SECONDS = {
            15, 30, 60, 120, 300, 900, 1800, 3600, 7200, 21600, 86400, 172800
    };

    public static Duration minSpan() {
        return MIN_SPAN;
    }

    public static Duration maxSpan() {
        return MAX_SPAN;
    }

    public static Duration stepFor(Duration span) {
        long targetStep = Math.max(MIN_STEP.getSeconds(), span.getSeconds() / MAX_POINTS);
        for (long nice : NICE_STEP_SECONDS) {
            if (nice >= targetStep) {
                return Duration.ofSeconds(nice);
            }
        }
        return Duration.ofSeconds(NICE_STEP_SECONDS[NICE_STEP_SECONDS.length - 1]);
    }
}
