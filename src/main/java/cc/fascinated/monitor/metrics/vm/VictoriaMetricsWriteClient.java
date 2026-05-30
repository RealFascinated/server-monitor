package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.exception.impl.InternalServerException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class VictoriaMetricsWriteClient {
    private final VictoriaMetricsProperties properties;
    private final HttpClient httpClient;

    public VictoriaMetricsWriteClient(VictoriaMetricsProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void flush(String body) {
        if (body.isEmpty()) {
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.properties.getImportUrl()))
                .header("Content-Type", "text/plain")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InternalServerException(
                        "VictoriaMetrics import failed with status %d: %s".formatted(response.statusCode(), response.body())
                );
            }
        } catch (InternalServerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalServerException("VictoriaMetrics import failed: %s".formatted(ex.getMessage()));
        }
    }
}
