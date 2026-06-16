package cc.fascinated.monitor.metrics.vm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.OptionalLong;

import static cc.fascinated.monitor.util.PrometheusTextMetrics.sumGaugeValues;

@Component
@Slf4j
public class VictoriaMetricsSelfMetricsClient {
    private final VictoriaMetricsProperties properties;
    private final HttpClient httpClient;

    public VictoriaMetricsSelfMetricsClient(VictoriaMetricsProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public OptionalLong storageSizeBytes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.properties.getMetricsUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("VictoriaMetrics /metrics returned status {}", response.statusCode());
                return OptionalLong.empty();
            }
            return sumGaugeValues(response.body(), "vm_data_size_bytes");
        } catch (Exception ex) {
            log.warn("Failed to read VictoriaMetrics storage size: {}", ex.getMessage());
            return OptionalLong.empty();
        }
    }
}
