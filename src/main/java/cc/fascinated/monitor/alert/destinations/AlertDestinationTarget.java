package cc.fascinated.monitor.alert.destinations;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** The only I/O primitive a destination's sender sees: a decrypted address plus a shared HTTP client. */
public class AlertDestinationTarget {
    private final HttpClient httpClient;
    private final URI uri;
    private final Duration timeout;

    public AlertDestinationTarget(HttpClient httpClient, URI uri, Duration timeout) {
        this.httpClient = httpClient;
        this.uri = uri;
        this.timeout = timeout;
    }

    public void postJson(String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.uri)
                .timeout(this.timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AlertDeliveryException("Interrupted while sending alert", false, null);
        } catch (Exception ex) {
            throw new AlertDeliveryException("Request failed: " + ex.getMessage(), true, null);
        }
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 429) {
            throw new AlertDeliveryException("Rate limited by destination", true, parseRetryAfter(response));
        }
        throw new AlertDeliveryException("Destination responded with status " + status, status >= 500, null);
    }

    private static Duration parseRetryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try {
                        return Duration.ofMillis((long) (Double.parseDouble(value.trim()) * 1000));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .orElse(null);
    }
}
