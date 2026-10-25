package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.destinations.AlertDestinationTarget;
import cc.fascinated.monitor.alert.destinations.AlertDeliveryException;
import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.persistance.AlertDestinationRow;
import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.repository.AlertDestinationRepository;
import cc.fascinated.monitor.service.EncryptionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Turns evaluator signals into persisted events and webhook deliveries on a single virtual-thread worker.
 * Signals arriving within the coalesce window go out together, one message per destination.
 */
@Slf4j
@Component
public class AlertDispatcher {
    private static final int MAX_BATCH = 100;
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(30);
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private final MonitorAlertProperties properties;
    private final AlertEventService eventService;
    private final AlertDestinationRepository destinationRepository;
    private final EncryptionService encryptionService;
    private final PlatformMetricsRecorder recorder;
    private final BlockingQueue<AlertSignal> queue;
    private final HttpClient httpClient;
    private volatile boolean running;
    private Thread worker;

    /** Delivery unit: the notification and the event it belongs to. */
    private record Pending(AlertNotification notification, long eventId) {}

    public AlertDispatcher(MonitorAlertProperties properties, AlertEventService eventService,
                           AlertDestinationRepository destinationRepository, EncryptionService encryptionService,
                           PlatformMetricsRecorder recorder) {
        this.properties = properties;
        this.eventService = eventService;
        this.destinationRepository = destinationRepository;
        this.encryptionService = encryptionService;
        this.recorder = recorder;
        this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getHttpTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @PostConstruct
    void start() {
        this.running = true;
        this.worker = Thread.ofVirtual().name("alert-dispatcher").start(this::run);
    }

    @PreDestroy
    void stop() {
        this.running = false;
        if (this.worker != null) {
            this.worker.interrupt();
        }
    }

    /** Never blocks: when the queue is full the signal is dropped and counted. */
    public void submit(AlertSignal signal) {
        if (!this.queue.offer(signal)) {
            this.recorder.recordAlertDropped();
            log.warn("Alert queue full, dropping {} for rule {}", signal.transition(), signal.ruleId());
        }
    }

    public int queueDepth() {
        return this.queue.size();
    }

    public AlertDestinationTarget targetFor(AlertDestinationRow destination) {
        String config = this.encryptionService.decrypt(destination.getConfigEncrypted());
        return new AlertDestinationTarget(this.httpClient, URI.create(config), this.properties.getHttpTimeout());
    }

    private void run() {
        while (this.running) {
            try {
                AlertSignal first = this.queue.poll(1, TimeUnit.SECONDS);
                if (first == null) {
                    continue;
                }
                List<AlertSignal> batch = new ArrayList<>();
                batch.add(first);
                collect(batch);
                process(batch);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                log.error("Alert dispatcher failed to process a batch", ex);
            }
        }
    }

    private void collect(List<AlertSignal> batch) throws InterruptedException {
        long deadline = System.nanoTime() + this.properties.getCoalesceWindow().toNanos();
        while (batch.size() < MAX_BATCH) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            AlertSignal next = this.queue.poll(remaining, TimeUnit.NANOSECONDS);
            if (next == null) {
                return;
            }
            batch.add(next);
        }
    }

    private void process(List<AlertSignal> batch) {
        Map<Long, List<Pending>> byDestination = new HashMap<>();
        for (AlertSignal signal : batch) {
            Optional<AlertEventRow> event = this.eventService.apply(signal);
            if (event.isEmpty() || signal.notification() == null || signal.destinationId() == null) {
                continue;
            }
            byDestination.computeIfAbsent(signal.destinationId(), ignored -> new ArrayList<>())
                    .add(new Pending(signal.notification(), event.get().getId()));
        }
        byDestination.forEach(this::deliver);
    }

    private void deliver(long destinationId, List<Pending> pending) {
        List<Long> eventIds = pending.stream().map(Pending::eventId).distinct().toList();
        Optional<AlertDestinationRow> destination = this.destinationRepository.findById(destinationId);
        if (destination.isEmpty()) {
            this.recorder.recordAlertNotification("failed");
            this.eventService.markFailed(eventIds, "Destination no longer exists");
            return;
        }
        List<AlertNotification> notifications = pending.stream().map(Pending::notification).toList();
        try {
            AlertDestinationTarget target = targetFor(destination.get());
            sendWithRetry(destination.get(), notifications, target);
            this.recorder.recordAlertNotification("sent");
            this.eventService.markNotified(eventIds, Instant.now());
        } catch (AlertDeliveryException ex) {
            log.warn("Alert delivery to destination {} failed: {}", destinationId, ex.getMessage());
            this.recorder.recordAlertNotification("failed");
            this.eventService.markFailed(eventIds, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Alert delivery to destination {} failed unexpectedly", destinationId, ex);
            this.recorder.recordAlertNotification("failed");
            this.eventService.markFailed(eventIds, "Unexpected error: " + ex.getMessage());
        }
    }

    private void sendWithRetry(AlertDestinationRow destination, List<AlertNotification> notifications,
                               AlertDestinationTarget target) {
        int attempts = Math.max(1, this.properties.getMaxDeliveryAttempts());
        for (int attempt = 1; ; attempt++) {
            try {
                destination.getType().send(notifications, target);
                return;
            } catch (AlertDeliveryException ex) {
                if (!ex.isRetryable() || attempt >= attempts) {
                    throw ex;
                }
                sleep(retryDelay(ex, attempt));
            }
        }
    }

    private static Duration retryDelay(AlertDeliveryException ex, int attempt) {
        Duration delay = ex.getRetryAfter() != null ? ex.getRetryAfter() : BASE_BACKOFF.multipliedBy(1L << (attempt - 1));
        return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AlertDeliveryException("Interrupted while retrying", false, null);
        }
    }
}
