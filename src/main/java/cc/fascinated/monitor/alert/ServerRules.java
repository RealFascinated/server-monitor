package cc.fascinated.monitor.alert;

import java.util.Map;

/** The rules that apply to one server, indexed by metric name. */
public record ServerRules(long serverId, String serverName, long loadedAtMillis, Map<String, RuleSnapshot[]> byMetric) {

    public boolean isEmpty() {
        return this.byMetric.isEmpty();
    }
}
