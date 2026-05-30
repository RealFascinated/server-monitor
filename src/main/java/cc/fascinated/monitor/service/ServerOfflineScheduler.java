package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class ServerOfflineScheduler {
    private final ServerRepository serverRepository;
    private final MonitorServerProperties serverProperties;

    public ServerOfflineScheduler(ServerRepository serverRepository, MonitorServerProperties serverProperties) {
        this.serverRepository = serverRepository;
        this.serverProperties = serverProperties;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    @Transactional
    public void markStaleServersOffline() {
        Instant cutoff = Instant.now().minus(this.serverProperties.getOfflineThreshold());
        int updated = this.serverRepository.markStaleServersOffline(cutoff);
        if (updated > 0) {
            log.info("Marked {} server(s) offline (no update since {})", updated, cutoff);
        }
    }
}
