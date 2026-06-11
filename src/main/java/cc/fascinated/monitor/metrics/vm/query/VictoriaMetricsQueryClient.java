package cc.fascinated.monitor.metrics.vm.query;

import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class VictoriaMetricsQueryClient {
    private final VictoriaMetricsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VictoriaMetricsQueryClient(VictoriaMetricsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public VmQueryResponse execute(VictoriaMetricsQuery query) {
        URI uri = query.isRange() ? buildRangeUri(query) : buildInstantUri(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InternalServerException(
                        "VictoriaMetrics query failed with status %d: %s".formatted(response.statusCode(), response.body())
                );
            }
            VmQueryResponse parsed = this.objectMapper.readValue(response.body(), VmQueryResponse.class);
            if (!"success".equals(parsed.status())) {
                throw new InternalServerException(
                        "VictoriaMetrics query failed: %s".formatted(parsed.error() != null ? parsed.error() : "unknown error")
                );
            }
            return parsed;
        } catch (InternalServerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalServerException("VictoriaMetrics query failed: %s".formatted(ex.getMessage()));
        }
    }

    private URI buildInstantUri(VictoriaMetricsQuery query) {
        StringBuilder url = new StringBuilder(this.properties.getQueryUrl());
        url.append("?query=").append(encode(query.promql()));
        url.append("&time=").append(query.at().getEpochSecond());
        return URI.create(url.toString());
    }

    private URI buildRangeUri(VictoriaMetricsQuery query) {
        StringBuilder url = new StringBuilder(this.properties.getQueryRangeUrl());
        url.append("?query=").append(encode(query.promql()));
        url.append("&start=").append(query.from().getEpochSecond());
        url.append("&end=").append(query.to().getEpochSecond());
        url.append("&step=").append(encode(query.stepParam()));
        return URI.create(url.toString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
