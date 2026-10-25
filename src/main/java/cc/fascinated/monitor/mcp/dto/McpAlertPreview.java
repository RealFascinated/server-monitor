package cc.fascinated.monitor.mcp.dto;

import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.dto.response.alert.AlertPreviewResponse;

import java.util.List;
import java.util.Map;

/**
 * A rule replayed over history, without the raw samples: just when it would have fired and how the metric behaved.
 * Times are epoch seconds. {@code timesFired} is not called {@code firingCount} because on rules and groups that
 * name means "firing right now".
 */
public record McpAlertPreview(
        AlertUnit unit,
        long from,
        long to,
        int stepSeconds,
        int timesFired,
        boolean truncated,
        List<Series> series
) {
    public record Series(
            Map<String, String> labels,
            double min,
            double max,
            double avg,
            List<AlertPreviewResponse.Interval> firing
    ) {}

    public static McpAlertPreview from(AlertPreviewResponse preview) {
        List<Series> series = preview.series().stream().map(McpAlertPreview::summarise).toList();
        return new McpAlertPreview(preview.unit(), preview.from(), preview.to(), preview.stepSeconds(),
                preview.firingCount(), preview.truncated(), series);
    }

    private static Series summarise(AlertPreviewResponse.PreviewSeries series) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        for (double value : series.values()) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        boolean empty = series.values().isEmpty();
        return new Series(series.labels(), empty ? 0 : round(min), empty ? 0 : round(max),
                empty ? 0 : round(sum / series.values().size()), series.firing());
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }
}
