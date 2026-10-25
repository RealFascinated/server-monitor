package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * A state change handed from the evaluator to the dispatcher. A signal without a notification closes the
 * open event silently (the series vanished or the rule changed).
 */
public record AlertSignal(
        AlertTransition transition,
        long ruleId,
        long serverId,
        String seriesKey,
        @Nullable Long destinationId,
        double peakValue,
        Instant at,
        @Nullable String resolveReason,
        @Nullable AlertNotification notification
) {}
