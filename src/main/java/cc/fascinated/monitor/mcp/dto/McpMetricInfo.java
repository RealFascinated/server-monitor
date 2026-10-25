package cc.fascinated.monitor.mcp.dto;

import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One queryable metric. {@code displayName}, {@code unit} and the suggested rule are only known for metrics a rule
 * can target; {@code series} is only present when the listing was scoped to a server. {@code displayName} is not
 * called {@code label} so a model does not mistake it for a series label.
 */
public record McpMetricInfo(
        String metric,
        String group,
        List<String> labelKeys,
        boolean alertable,
        @Nullable String displayName,
        @Nullable AlertUnit unit,
        @Nullable AlertOperator suggestedOperator,
        @Nullable Double suggestedThreshold,
        @Nullable List<Map<String, String>> series
) {}
