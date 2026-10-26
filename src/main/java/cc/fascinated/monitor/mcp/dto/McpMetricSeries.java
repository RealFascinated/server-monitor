package cc.fascinated.monitor.mcp.dto;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One labelled series. The statistics are taken over the downsampled samples, one per step, so a spike shorter than
 * the step can be missing from {@code max}. {@code maxAt} and {@code minAt} are epoch seconds of the first sample
 * holding that value. {@code points} ({@code [epochSeconds, value]}) is only present when the caller asked for it.
 */
public record McpMetricSeries(
        Map<String, String> labels,
        double min,
        double max,
        double avg,
        double last,
        long maxAt,
        long minAt,
        @Nullable List<Object[]> points
) {}
