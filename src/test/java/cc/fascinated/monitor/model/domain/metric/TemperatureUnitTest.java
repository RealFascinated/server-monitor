package cc.fascinated.monitor.model.domain.metric;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemperatureUnitTest {
    @Test
    void convertsFromCelsius() {
        assertThat(TemperatureUnit.CELSIUS.fromCelsius(21.5)).isEqualTo(21.5);
        assertThat(TemperatureUnit.FAHRENHEIT.fromCelsius(100)).isEqualTo(212);
        assertThat(TemperatureUnit.FAHRENHEIT.fromCelsius(-40)).isEqualTo(-40);
    }

    @Test
    void unknownKeysFallBackToCelsius() {
        assertThat(TemperatureUnit.fromKey("fahrenheit")).isEqualTo(TemperatureUnit.FAHRENHEIT);
        assertThat(TemperatureUnit.fromKey("kelvin")).isEqualTo(TemperatureUnit.CELSIUS);
        assertThat(TemperatureUnit.fromKey(null)).isEqualTo(TemperatureUnit.CELSIUS);
    }
}
