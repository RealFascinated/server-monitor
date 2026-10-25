package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Seeds firing state from open events on startup so a restart does not notify for alerts that never cleared. */
@Slf4j
@Component
public class AlertStateRehydrator {
    private final AlertEventService eventService;
    private final AlertRuleRepository ruleRepository;
    private final AlertEvaluator evaluator;

    public AlertStateRehydrator(AlertEventService eventService, AlertRuleRepository ruleRepository,
                                AlertEvaluator evaluator) {
        this.eventService = eventService;
        this.ruleRepository = ruleRepository;
        this.evaluator = evaluator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rehydrate() {
        try {
            List<AlertEventRow> open = this.eventService.openEvents();
            if (open.isEmpty()) {
                return;
            }
            Set<Long> ruleIds = open.stream().map(AlertEventRow::getRuleId).collect(Collectors.toSet());
            Map<Long, AlertRuleRow> rules = new HashMap<>();
            for (AlertRuleRow rule : this.ruleRepository.findAllById(ruleIds)) {
                rules.put(rule.getId(), rule);
            }
            int restored = 0;
            for (AlertEventRow event : open) {
                AlertRuleRow rule = rules.get(event.getRuleId());
                if (rule == null || !rule.isEnabled()) {
                    this.eventService.closeOpen(List.of(event.getRuleId()), AlertEventRow.REASON_RULE_CHANGED);
                    continue;
                }
                this.evaluator.restore(rule.getId(), event.getServerId(), event.getSeriesKey(), rule.getMetric(),
                        rule.getWindowSeconds(), event.getStartedAt(), event.getPeakValue(), event.getLastNotifiedAt());
                restored++;
            }
            log.info("Restored {} firing alert(s) from open events", restored);
        } catch (RuntimeException ex) {
            log.error("Could not restore alert state", ex);
        }
    }
}
