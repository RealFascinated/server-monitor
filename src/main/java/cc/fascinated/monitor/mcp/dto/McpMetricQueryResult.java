package cc.fascinated.monitor.mcp.dto;

import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code from} and {@code to} are epoch seconds, like the points. */
public record McpMetricQueryResult(
        String metric,
        @Nullable String displayName,
        @Nullable AlertUnit unit,
        long from,
        long to,
        int stepSeconds,
        List<McpMetricSeries> series,
        /** True when the metric has more series than are returned; narrow it with {@code labels}. */
        boolean truncated
) {}
