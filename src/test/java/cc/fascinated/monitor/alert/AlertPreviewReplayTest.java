package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.RuleSpec;
import cc.fascinated.monitor.metrics.vm.query.VmSample;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.dto.response.alert.AlertPreviewResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertPreviewReplayTest {
    private static VmTimeSeries series(double... values) {
        List<VmSample> samples = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            samples.add(new VmSample(Instant.ofEpochSecond(1000 + i * 10L), values[i]));
        }
        return new VmTimeSeries(Map.of("server_id", "1", "disk", "/"), samples);
    }

    @Test
    void reportsClosedAndOngoingFiringIntervals() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, null, 0);

        AlertPreviewResponse.PreviewSeries replayed = AlertPreviewService.replay(series(10, 95, 99, 20, 10, 97), spec);

        assertThat(replayed.labels()).isEqualTo(Map.of("disk", "/"));
        assertThat(replayed.firing()).containsExactly(
                new AlertPreviewResponse.Interval(1010, 1030, false, 99),
                new AlertPreviewResponse.Interval(1050, 1050, true, 97));
        assertThat(replayed.timestamps()).hasSize(6);
    }

    @Test
    void usesTheSameWindowSemanticsAsLiveEvaluation() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 50, null, 100);
        double[] spike = new double[25];
        spike[12] = 100;

        assertThat(AlertPreviewService.replay(series(spike), spec).firing()).isEmpty();
    }
}
