package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class VictoriaMetricsWriteClient {
    private final VictoriaMetricsProperties properties;
    private final HttpClient httpClient;
    private final PlatformMetricsRecorder platformMetricsRecorder;

    public VictoriaMetricsWriteClient(VictoriaMetricsProperties properties,
                                      PlatformMetricsRecorder platformMetricsRecorder) {
        this.properties = properties;
        this.platformMetricsRecorder = platformMetricsRecorder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void flush(String body) {
        if (body.isEmpty()) {
            return;
        }
        long startedNanos = System.nanoTime();
        boolean success = false;
        try {
            flushInternal(body);
            success = true;
        } finally {
            this.platformMetricsRecorder.recordVmWrite((System.nanoTime() - startedNanos) / 1_000_000_000.0, success);
        }
    }

    private void flushInternal(String body) {
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

    public void deleteSeriesForServer(long serverId) {
        String match = "{server_id=\"%d\"}".formatted(serverId);
        String body = "match[]=" + URLEncoder.encode(match, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.deleteSeriesUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InternalServerException(
                        "VictoriaMetrics delete_series failed with status %d: %s".formatted(response.statusCode(), response.body())
                );
            }
        } catch (InternalServerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalServerException("VictoriaMetrics delete_series failed: %s".formatted(ex.getMessage()));
        }
    }

    private URI deleteSeriesUri() {
        String url = this.properties.getDeleteSeriesUrl();
        String authKey = this.properties.getDeleteToken();
        if (authKey == null || authKey.isEmpty()) {
            return URI.create(url);
        }
        String separator = url.contains("?") ? "&" : "?";
        return URI.create(url + separator + "authKey=" + URLEncoder.encode(authKey, StandardCharsets.UTF_8));
    }
}
