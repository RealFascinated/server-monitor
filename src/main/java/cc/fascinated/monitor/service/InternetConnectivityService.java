package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class InternetConnectivityService {
    private final MonitorServerProperties serverProperties;
    private final HttpClient httpClient;

    public InternetConnectivityService(MonitorServerProperties serverProperties) {
        this.serverProperties = serverProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(serverProperties.getConnectivityTimeout())
                .build();
    }

    public boolean isAvailable() {
        String probeUrl = this.serverProperties.getConnectivityProbeUrl();
        if (probeUrl == null || probeUrl.isBlank()) {
            return true;
        }
        Duration timeout = this.serverProperties.getConnectivityTimeout();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(probeUrl))
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return true;
            }
            log.warn("Internet connectivity probe returned status {}", response.statusCode());
            return false;
        } catch (Exception ex) {
            log.warn("Internet connectivity probe failed: {}", ex.getMessage());
            return false;
        }
    }
}
