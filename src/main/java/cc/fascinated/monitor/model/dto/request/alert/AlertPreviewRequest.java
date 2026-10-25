package cc.fascinated.monitor.model.dto.request.alert;

import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** A draft rule plus the server whose history it is replayed against. */
public record AlertPreviewRequest(
        @NotNull(message = "Server is required")
        Long serverId,
        @NotBlank(message = "Metric is required")
        String metric,
        @Nullable Map<String, String> labelFilter,
        @NotNull(message = "Operator is required")
        AlertOperator operator,
        @NotNull(message = "Threshold is required")
        Double threshold,
        @Nullable Double recoveryThreshold,
        @Nullable Integer windowSeconds,
        @Min(value = 1, message = "Range must be at least 1 hour")
        @Max(value = 168, message = "Range must be at most 168 hours")
        int rangeHours
) {}
