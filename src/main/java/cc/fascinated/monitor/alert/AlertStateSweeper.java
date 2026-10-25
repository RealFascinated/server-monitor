package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Periodic alert housekeeping: keeps "server offline" rules evaluating (an offline server sends no samples to
 * drive them), closes series that stopped reporting and sends repeat reminders.
 */
@Slf4j
@Component
public class AlertStateSweeper {
    private final AlertEvaluator evaluator;
    private final ServerRepository serverRepository;

    public AlertStateSweeper(AlertEvaluator evaluator, ServerRepository serverRepository) {
        this.evaluator = evaluator;
        this.serverRepository = serverRepository;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    public void sweep() {
        try {
            long nowMillis = System.currentTimeMillis();
            long nowSeconds = Instant.ofEpochMilli(nowMillis).getEpochSecond();
            for (ServerRow server : this.serverRepository.findByStatus(ServerStatus.OFFLINE)) {
                if (this.evaluator.hasServerUpRules(server.getId())) {
                    this.evaluator.observeServerUp(server.getId(), nowSeconds, 0);
                }
            }
            this.evaluator.sweep(nowMillis);
        } catch (RuntimeException ex) {
            log.error("Alert sweep failed", ex);
        }
    }
}
