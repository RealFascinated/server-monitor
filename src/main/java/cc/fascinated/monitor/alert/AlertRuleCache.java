package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Per-server rule snapshots (the rules of every group the server is in) so the ingest path never queries the database. Empty snapshots are cached too,
 * which makes a server with no alerts cost one map lookup per ingest.
 */
@Component
public class AlertRuleCache {
    private final AlertRuleRepository ruleRepository;
    private final ServerRepository serverRepository;
    private final AlertGroupRepository groupRepository;
    private final MonitorAlertProperties properties;
    private final ConcurrentHashMap<Long, ServerRules> cache = new ConcurrentHashMap<>();

    public AlertRuleCache(AlertRuleRepository ruleRepository, ServerRepository serverRepository,
                          AlertGroupRepository groupRepository, MonitorAlertProperties properties) {
        this.groupRepository = groupRepository;
        this.ruleRepository = ruleRepository;
        this.serverRepository = serverRepository;
        this.properties = properties;
    }

    public ServerRules get(long serverId) {
        ServerRules cached = this.cache.get(serverId);
        if (cached != null && System.currentTimeMillis() - cached.loadedAtMillis() < this.properties.getCacheTtl().toMillis()) {
            return cached;
        }
        ServerRules loaded = load(serverId);
        this.cache.put(serverId, loaded);
        return loaded;
    }

    public void invalidate(long serverId) {
        this.cache.remove(serverId);
    }

    public void invalidateAll() {
        this.cache.clear();
    }

    private ServerRules load(long serverId) {
        List<AlertRuleRow> rows = this.ruleRepository.findApplicableToServer(serverId);
        long now = System.currentTimeMillis();
        if (rows.isEmpty()) {
            return new ServerRules(serverId, "", now, Map.of());
        }
        String serverName = this.serverRepository.findById(serverId).map(ServerRow::getServerName).orElse("Server " + serverId);
        Map<Long, Long> destinations = new HashMap<>();
        for (AlertGroupRow group : this.groupRepository.findAllById(rows.stream().map(AlertRuleRow::getGroupId).collect(Collectors.toSet()))) {
            destinations.put(group.getId(), group.getDestinationId());
        }
        Map<String, List<RuleSnapshot>> grouped = new HashMap<>();
        for (AlertRuleRow row : rows) {
            if (row.isEnabled()) {
                grouped.computeIfAbsent(row.getMetric(), ignored -> new ArrayList<>())
                        .add(RuleSnapshot.from(row, destinations.get(row.getGroupId())));
            }
        }
        Map<String, RuleSnapshot[]> byMetric = new HashMap<>(grouped.size());
        grouped.forEach((metric, rules) -> byMetric.put(metric, rules.toArray(RuleSnapshot[]::new)));
        return new ServerRules(serverId, serverName, now, Map.copyOf(byMetric));
    }
}
