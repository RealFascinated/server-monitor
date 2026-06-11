package cc.fascinated.monitor.metrics.vm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ServerMetricGroups {
    HOST("monitor_host_.+", "monitor_host_cpu_core_pct|monitor_host_temperature_celsius", MetricSection.HOST, null, new String[0]),
    CPU_CORE("monitor_host_cpu_core_pct", null, MetricSection.CPU_CORES, null, new String[]{"cpu"}),
    DISK("monitor_disk_.+", null, MetricSection.DISKS, null, new String[]{"disk"}),
    DISK_ETA_UNTIL_FULL(null, null, MetricSection.DISKS, "etaUntilFull", new String[]{"disk"}),
    NETWORK("monitor_net_.+", null, MetricSection.NETWORKS, null, new String[]{"interface"}),
    GPU("monitor_gpu_.+", null, MetricSection.GPUS, null, new String[]{"device_id", "gpu", "vendor"}, new String[]{"device_id"}),
    DOCKER("monitor_container_.+", null, MetricSection.CONTAINERS, null, new String[]{"container"}),
    TEMPERATURE("monitor_host_temperature_celsius", null, MetricSection.TEMPERATURES, null, new String[]{"sensor"}),
    ZFS_ARC("monitor_zfs_arc_.+", null, MetricSection.ZFS_ARC, null, new String[0]),
    ZFS_POOL("monitor_zfs_pool_.+", null, MetricSection.ZFS_POOLS, null, new String[]{"pool", "health", "scan_state"}),
    TCP("monitor_tcp_connections", null, MetricSection.TCP_CONNECTIONS, null, new String[]{"state"});

    @Getter(AccessLevel.NONE)
    private final String nameRegex;
    @Getter(AccessLevel.NONE)
    private final String excludeRegex;
    private final MetricSection section;
    private final String mergeField;
    private final String[] identityLabels;
    private final String[] groupingLabels;

    ServerMetricGroups(String nameRegex, String excludeRegex, MetricSection section, String mergeField, String[] identityLabels) {
        this(nameRegex, excludeRegex, section, mergeField, identityLabels, identityLabels);
    }

    ServerMetricGroups(String nameRegex, String excludeRegex, MetricSection section, String mergeField,
                       String[] identityLabels, String[] groupingLabels) {
        this.nameRegex = nameRegex;
        this.excludeRegex = excludeRegex;
        this.section = section;
        this.mergeField = mergeField;
        this.identityLabels = identityLabels;
        this.groupingLabels = groupingLabels;
    }

    public String promql(long serverId) {
        if (this == DISK_ETA_UNTIL_FULL) {
            return diskEtaUntilFullPromql(serverId);
        }
        StringBuilder query = new StringBuilder("{__name__=~\"");
        query.append(this.nameRegex);
        query.append("\",server_id=\"");
        query.append(serverId);
        query.append('"');
        if (this.excludeRegex != null) {
            query.append(",__name__!~\"");
            query.append(this.excludeRegex);
            query.append('"');
        }
        query.append('}');
        return query.toString();
    }

    private static String diskEtaUntilFullPromql(long serverId) {
        String selector = "{server_id=\"" + serverId + "\"}";
        String total = "monitor_disk_total_bytes" + selector;
        String used = "monitor_disk_used_bytes" + selector;
        return "(" + total + " - " + used + ") / clamp_min((" + used + " - " + used + " offset 1d) / 86400, 1)";
    }
}
