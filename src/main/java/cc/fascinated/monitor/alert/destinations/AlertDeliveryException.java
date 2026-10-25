package cc.fascinated.monitor.alert.destinations;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

@Getter
public class AlertDeliveryException extends RuntimeException {
    private final boolean retryable;
    @Nullable
    private final Duration retryAfter;

    public AlertDeliveryException(String message, boolean retryable, @Nullable Duration retryAfter) {
        super(message);
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }
}
