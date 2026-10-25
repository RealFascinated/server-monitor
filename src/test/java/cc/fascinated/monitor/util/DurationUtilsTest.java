package cc.fascinated.monitor.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationUtilsTest {
    @Test
    void formatsCompactly() {
        assertThat(DurationUtils.format(Duration.ofSeconds(45))).isEqualTo("45s");
        assertThat(DurationUtils.format(Duration.ofMinutes(12))).isEqualTo("12m");
        assertThat(DurationUtils.format(Duration.ofMinutes(90))).isEqualTo("1h 30m");
        assertThat(DurationUtils.format(Duration.ofHours(48))).isEqualTo("2d");
        assertThat(DurationUtils.format(Duration.ofHours(51))).isEqualTo("2d 3h");
        assertThat(DurationUtils.format(Duration.ofSeconds(-5))).isEqualTo("0s");
    }
}
