package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import cc.fascinated.monitor.repository.ServerMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Seeds a new user with a "Default" alert group holding the rules every series flags through
 * {@link AlertInfo#defaultRule()} (CPU, memory, disk, temperature, ...). The group picks up every server the user
 * gains access to. The group starts disabled so nothing fires until the user opts in. Rules have no destination
 * until the user adds one, but still record history and show in the firing count once the group is enabled. Depends on repositories only, so auth can call it without a cycle.
 */
@Slf4j
@Service
public class DefaultAlertRuleService {
    static final String DEFAULT_GROUP_NAME = "Default";

    private final AlertGroupRepository groupRepository;
    private final AlertRuleRepository ruleRepository;
    private final ServerMemberRepository memberRepository;
    private final TransactionTemplate transactionTemplate;

    public DefaultAlertRuleService(AlertGroupRepository groupRepository, AlertRuleRepository ruleRepository,
                                   ServerMemberRepository memberRepository, TransactionTemplate transactionTemplate) {
        this.groupRepository = groupRepository;
        this.ruleRepository = ruleRepository;
        this.memberRepository = memberRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void createFor(long userId) {
        createFor(userId, List.of());
    }

    /**
     * Accounts that predate alerting never went through registration, so give each of them the default group
     * (holding the servers they already have). Users that already have one are left alone, so rules someone
     * deleted from theirs are never brought back.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissing() {
        for (Long userId : this.groupRepository.findUserIdsWithoutDefaultGroup()) {
            try {
                this.transactionTemplate.executeWithoutResult(status ->
                        createFor(userId, this.memberRepository.findServerIdsByUserId(userId)));
                log.info("Created the default alert group for user {}", userId);
            } catch (RuntimeException e) {
                log.warn("Could not create the default alert group for user {}", userId, e);
            }
        }
    }

    private void createFor(long userId, Collection<Long> serverIds) {
        Instant now = Instant.now();
        AlertGroupRow row = AlertGroupRow.defaultGroup(userId, DEFAULT_GROUP_NAME, now);
        row.getServerIds().addAll(serverIds);
        AlertGroupRow group = this.groupRepository.save(row);
        List<AlertRuleRow> rules = VmMetricCatalog.alertable().values().stream()
                .filter(series -> series.alertInfo() != null && series.alertInfo().defaultRule() != null)
                .sorted(Comparator.comparing(VmGaugeSeries::metricName))
                .map(series -> toRule(userId, group.getId(), series, now))
                .toList();
        this.ruleRepository.saveAll(rules);
    }

    private static AlertRuleRow toRule(long userId, long groupId, VmGaugeSeries series, Instant now) {
        AlertInfo info = series.alertInfo();
        AlertInfo.DefaultRule defaults = info.defaultRule();
        AlertRuleRow row = new AlertRuleRow(userId, groupId, now);
        row.setName(info.label());
        row.setMetric(series.metricName());
        row.setLabelFilter(Map.of());
        row.setOperator(info.operator());
        row.setThreshold(info.threshold());
        row.setWindowSeconds(defaults.windowSeconds());
        row.setSeverity(defaults.severity());
        row.setEnabled(true);
        return row;
    }
}
