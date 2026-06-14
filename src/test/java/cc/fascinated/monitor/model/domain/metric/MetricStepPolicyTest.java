package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricStepPolicyTest {
    @Test
    void stepFor_oneHour_returns15Seconds() {
        assertEquals(Duration.ofSeconds(15), MetricStepPolicy.stepFor(Duration.ofHours(1)));
    }

    @Test
    void stepFor_sevenDays_returns30Minutes() {
        assertEquals(Duration.ofMinutes(30), MetricStepPolicy.stepFor(Duration.ofDays(7)));
    }

    @Test
    void stepFor_oneYear_returnsOneDay() {
        assertEquals(Duration.ofDays(1), MetricStepPolicy.stepFor(Duration.ofDays(365)));
    }

    @Test
    void stepFor_boundarySnapsToNiceInterval() {
        Duration span = Duration.ofSeconds(400 * 60);
        assertEquals(Duration.ofMinutes(1), MetricStepPolicy.stepFor(span));
    }

    @Test
    void parse_rejectsSpanShorterThanFiveMinutes() {
        long to = Instant.now().getEpochSecond();
        long from = to - Duration.ofMinutes(4).getSeconds();
        assertThrows(BadRequestException.class, () -> MetricQueryWindow.parse(from, to));
    }

    @Test
    void parse_rejectsSpanLongerThanTwoYears() {
        long to = Instant.now().getEpochSecond();
        long from = to - Duration.ofDays(731).getSeconds();
        assertThrows(BadRequestException.class, () -> MetricQueryWindow.parse(from, to));
    }

    @Test
    void parse_clampsFutureTo() {
        long from = Instant.now().minus(Duration.ofHours(1)).getEpochSecond();
        long to = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();
        MetricQueryWindow window = MetricQueryWindow.parse(from, to);
        assertEquals(Duration.ofSeconds(15), window.step());
        assert window.to().isBefore(Instant.now().plusSeconds(1));
    }
}
