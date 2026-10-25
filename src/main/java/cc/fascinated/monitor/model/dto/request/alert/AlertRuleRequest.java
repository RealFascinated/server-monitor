package cc.fascinated.monitor.model.dto.request.alert;

import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record AlertRuleRequest(
        @NotNull(message = "Group is required")
        Long groupId,
        @Nullable
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,
        @NotBlank(message = "Metric is required")
        String metric,
        @Nullable Map<String, String> labelFilter,
        @NotNull(message = "Operator is required")
        AlertOperator operator,
        @NotNull(message = "Threshold is required")
        Double threshold,
        @Nullable Double recoveryThreshold,
        @Nullable Integer windowSeconds,
        @NotNull(message = "Severity is required")
        AlertSeverity severity,
        @Nullable Integer repeatIntervalSeconds,
        @Nullable Boolean enabled
) {}
