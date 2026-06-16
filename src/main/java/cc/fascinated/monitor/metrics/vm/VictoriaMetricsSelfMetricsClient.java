package cc.fascinated.monitor.metrics.vm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

import static cc.fascinated.monitor.util.PrometheusTextMetrics.sumGaugeValues;
import static cc.fascinated.monitor.util.PrometheusTextMetrics.sumGaugeValuesExcludingLabelPrefix;

@Component
@Slf4j
public class VictoriaMetricsSelfMetricsClient {
    public record SelfMetrics(OptionalLong storageSizeBytes, OptionalLong datapointCount) {}

    private final VictoriaMetricsProperties properties;
    private final HttpClient httpClient;

    public VictoriaMetricsSelfMetricsClient(VictoriaMetricsProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public SelfMetrics read() {
        Optional<String> body = fetchMetricsBody();
        if (body.isEmpty()) {
            return new SelfMetrics(OptionalLong.empty(), OptionalLong.empty());
        }
        String metrics = body.get();
        return new SelfMetrics(
                sumGaugeValues(metrics, "vm_data_size_bytes"),
                sumGaugeValuesExcludingLabelPrefix(metrics, "vm_rows", "type", "indexdb")
        );
    }

    private Optional<String> fetchMetricsBody() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.properties.getMetricsUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("VictoriaMetrics /metrics returned status {}", response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Exception ex) {
            log.warn("Failed to read VictoriaMetrics self metrics: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
