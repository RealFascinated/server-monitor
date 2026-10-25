package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.model.domain.alert.AlertUnit;

import java.util.List;
import java.util.Map;

public record AlertPreviewResponse(
        AlertUnit unit,
        long from,
        long to,
        int stepSeconds,
        List<PreviewSeries> series,
        int firingCount,
        boolean truncated
) {
    public record PreviewSeries(
            Map<String, String> labels,
            List<Long> timestamps,
            List<Double> values,
            List<Interval> firing
    ) {}

    public record Interval(long from, long to, boolean ongoing, double peak) {}
}
