package cc.fascinated.monitor.model.domain.alert;

import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertUnitTest {
    @Test
    void formatsEachUnit() {
        assertThat(AlertUnit.PERCENT.format(96.55)).isEqualTo("96.55%");
        assertThat(AlertUnit.PERCENT.format(90)).isEqualTo("90%");
        assertThat(AlertUnit.BYTES.format(1536)).isEqualTo("1.5 KB");
        assertThat(AlertUnit.BYTES.format(512)).isEqualTo("512 B");
        assertThat(AlertUnit.BYTES_PER_SECOND.format(5L * 1024 * 1024)).isEqualTo("5 MB/s");
        assertThat(AlertUnit.TEMPERATURE.format(71.25)).isEqualTo("71.3°C");
        assertThat(AlertUnit.MILLISECONDS.format(12)).isEqualTo("12 ms");
        assertThat(AlertUnit.COUNT.format(3)).isEqualTo("3");
    }

    @Test
    void temperaturesFollowTheReadersUnit() {
        assertThat(AlertUnit.TEMPERATURE.format(100, TemperatureUnit.FAHRENHEIT)).isEqualTo("212°F");
        assertThat(AlertUnit.TEMPERATURE.format(0, TemperatureUnit.FAHRENHEIT)).isEqualTo("32°F");
        assertThat(AlertUnit.PERCENT.format(50, TemperatureUnit.FAHRENHEIT)).isEqualTo("50%");
    }

    @Test
    void stateReadsAsHealthyOrUnhealthy() {
        assertThat(AlertUnit.STATE.format(1)).isEqualTo("healthy");
        assertThat(AlertUnit.STATE.format(0)).isEqualTo("unhealthy");
    }

    @Test
    void operatorsCompareStrictly() {
        assertThat(AlertOperator.MORE_THAN.test(91, 90)).isTrue();
        assertThat(AlertOperator.MORE_THAN.test(90, 90)).isFalse();
        assertThat(AlertOperator.LESS_THAN.test(9, 10)).isTrue();
        assertThat(AlertOperator.LESS_THAN.test(10, 10)).isFalse();
        assertThat(AlertOperator.MORE_THAN.isWorse(95, 92)).isTrue();
        assertThat(AlertOperator.LESS_THAN.isWorse(1, 2)).isTrue();
    }
}
