package cc.fascinated.monitor.mcp.dto;

import java.util.List;
import java.util.Map;

/**
 * One labelled series with its downsampled {@code [epochSeconds, value]} points. The statistics are taken over those
 * points, one sample per step, so a spike shorter than the step can be missing from {@code max}.
 */
public record McpMetricSeries(
        Map<String, String> labels,
        double min,
        double max,
        double avg,
        double last,
        List<double[]> points
) {}
