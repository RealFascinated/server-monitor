package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.repository.AlertEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence of alert events, called from the dispatcher worker (never from the ingest thread). */
@Service
public class AlertEventService {
    private static final int MAX_ERROR_LENGTH = 500;

    private final AlertEventRepository eventRepository;

    public AlertEventService(AlertEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Applies a signal to the event log. Returns the event the notification belongs to, or empty when
     * nothing should be sent (a duplicate firing, or a resolve for an event we never recorded).
     */
    @Transactional
    public Optional<AlertEventRow> apply(AlertSignal signal) {
        Optional<AlertEventRow> open = this.eventRepository
                .findFirstByRuleIdAndServerIdAndSeriesKeyAndResolvedAtIsNull(
                        signal.ruleId(), signal.serverId(), signal.seriesKey());
        switch (signal.transition()) {
            case FIRING -> {
                if (open.isPresent()) {
                    return Optional.empty();
                }
                return Optional.of(this.eventRepository.save(new AlertEventRow(
                        signal.ruleId(), signal.serverId(), signal.seriesKey(), signal.at(), signal.peakValue())));
            }
            case REPEAT -> {
                open.ifPresent(event -> event.setPeakValue(Math.max(event.getPeakValue(), signal.peakValue())));
                return open;
            }
            case ESCALATED -> {
                open.ifPresent(event -> {
                    event.setPeakValue(Math.max(event.getPeakValue(), signal.peakValue()));
                    event.setSilencedAt(null);
                    event.setSilencedValue(null);
                });
                return open;
            }
            case RESOLVED -> {
                open.ifPresent(event -> {
                    event.setResolvedAt(signal.at());
                    event.setResolveReason(signal.resolveReason());
                    event.setPeakValue(signal.peakValue());
                });
                return signal.notification() == null ? Optional.empty() : open;
            }
            default -> throw new IllegalStateException("Unhandled transition " + signal.transition());
        }
    }

    @Transactional
    public void markNotified(Collection<Long> eventIds, Instant at) {
        for (AlertEventRow event : this.eventRepository.findAllById(eventIds)) {
            event.setLastNotifiedAt(at);
            event.setNotifyError(null);
        }
    }

    @Transactional
    public void markFailed(Collection<Long> eventIds, String error) {
        String trimmed = error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
        for (AlertEventRow event : this.eventRepository.findAllById(eventIds)) {
            event.setNotifyError(trimmed);
        }
    }

    @Transactional
    public int closeOpen(Collection<Long> ruleIds, String reason) {
        if (ruleIds.isEmpty()) {
            return 0;
        }
        return this.eventRepository.closeOpenEvents(ruleIds, Instant.now(), reason);
    }

    /** Closes the open events of some rules on one server, e.g. when it leaves their group. */
    @Transactional
    public int closeOpenForServer(Collection<Long> ruleIds, long serverId, String reason) {
        if (ruleIds.isEmpty()) {
            return 0;
        }
        return this.eventRepository.closeOpenEventsForServer(ruleIds, serverId, Instant.now(), reason);
    }

    @Transactional(readOnly = true)
    public List<AlertEventRow> openEvents() {
        return this.eventRepository.findByResolvedAtIsNull();
    }
}
