package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.query.VmSample;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public final class MetricTimeGrid {
    private final List<Long> timestamps;
    private final long stepSeconds;
    private final long gridStart;

    private MetricTimeGrid(List<Long> timestamps, long stepSeconds, long gridStart) {
        this.timestamps = timestamps;
        this.stepSeconds = stepSeconds;
        this.gridStart = gridStart;
    }

    public static MetricTimeGrid from(MetricTimeRange.QueryWindow window) {
        long step = window.stepSeconds();
        long start = window.from().getEpochSecond();
        long end = window.to().getEpochSecond();
        start -= start % step;
        List<Long> timestamps = new ArrayList<>();
        for (long timestamp = start; timestamp <= end; timestamp += step) {
            timestamps.add(timestamp);
        }
        return new MetricTimeGrid(timestamps, step, start);
    }

    public List<Double> align(VmTimeSeries series) {
        List<Double> values = new ArrayList<>(this.timestamps.size());
        for (int index = 0; index < this.timestamps.size(); index++) {
            values.add(null);
        }

        for (VmSample sample : series.samples()) {
            long timestamp = sample.timestamp().getEpochSecond();
            if (timestamp < this.gridStart) {
                continue;
            }

            int index = (int) ((timestamp - this.gridStart) / this.stepSeconds);
            if (index < 0 || index >= values.size()) {
                continue;
            }

            double value = sample.value();
            if (Double.isFinite(value)) {
                values.set(index, value);
            }
        }

        return values;
    }
}
