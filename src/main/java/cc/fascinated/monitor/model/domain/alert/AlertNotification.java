package cc.fascinated.monitor.model.domain.alert;

import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record AlertNotification(
        long ruleId,
        @Nullable String ruleName,
        long serverId,
        String serverName,
        String serverUrl,
        String metric,
        String metricLabel,
        Map<String, String> labels,
        AlertOperator operator,
        double threshold,
        double value,
        AlertUnit unit,
        TemperatureUnit temperatureUnit,
        AlertSeverity severity,
        AlertTransition transition,
        int windowSeconds,
        @Nullable Duration firedFor,
        Instant occurredAt
) {}
