package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Seeds a new user with a "Default" alert group holding the rules every series flags through
 * {@link AlertInfo#defaultRule()} (CPU, memory, disk, temperature, ...). The group picks up every server the user
 * gains access to. The group starts disabled so nothing fires until the user opts in. Rules have no destination
 * until the user adds one, but still record history and show in the firing count once the group is enabled. Depends on repositories only, so auth can call it without a cycle.
 */
@Service
public class DefaultAlertRuleService {
    static final String DEFAULT_GROUP_NAME = "Default";

    private final AlertGroupRepository groupRepository;
    private final AlertRuleRepository ruleRepository;

    public DefaultAlertRuleService(AlertGroupRepository groupRepository, AlertRuleRepository ruleRepository) {
        this.groupRepository = groupRepository;
        this.ruleRepository = ruleRepository;
    }

    public void createFor(long userId) {
        Instant now = Instant.now();
        AlertGroupRow group = this.groupRepository.save(AlertGroupRow.defaultGroup(userId, DEFAULT_GROUP_NAME, now));
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
