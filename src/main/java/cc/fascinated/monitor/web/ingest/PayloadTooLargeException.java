package cc.fascinated.monitor.web.ingest;

import java.io.IOException;

/**
 * Thrown while reading an ingest request body that grows past the configured limit.
 */
public class PayloadTooLargeException extends IOException {
    public PayloadTooLargeException(long limitBytes) {
        super("Request body exceeds the limit of %d bytes".formatted(limitBytes));
    }
}
