package cc.fascinated.monitor.metrics.vm;

public enum ServerMetricGroups {
    HOST("monitor_host_.+", "monitor_host_cpu_core_pct|monitor_host_temperature_celsius", MetricSection.HOST, null),
    CPU_CORE("monitor_host_cpu_core_pct", null, MetricSection.CPU_CORES, null),
    DISK("monitor_disk_.+", null, MetricSection.DISKS, null),
    DISK_ETA_UNTIL_FULL(null, null, MetricSection.DISKS, "etaUntilFull"),
    NETWORK("monitor_net_.+", null, MetricSection.NETWORKS, null),
    GPU("monitor_gpu_.+", null, MetricSection.GPUS, null),
    DOCKER("monitor_container_.+", null, MetricSection.CONTAINERS, null),
    TEMPERATURE("monitor_host_temperature_celsius", null, MetricSection.TEMPERATURES, null),
    ZFS_ARC("monitor_zfs_arc_.+", null, MetricSection.ZFS_ARC, null),
    ZFS_POOL("monitor_zfs_pool_.+", null, MetricSection.ZFS_POOLS, null),
    TCP("monitor_tcp_connections", null, MetricSection.TCP_CONNECTIONS, null);

    private final String nameRegex;
    private final String excludeRegex;
    private final MetricSection section;
    private final String mergeField;

    ServerMetricGroups(String nameRegex, String excludeRegex, MetricSection section, String mergeField) {
        this.nameRegex = nameRegex;
        this.excludeRegex = excludeRegex;
        this.section = section;
        this.mergeField = mergeField;
    }

    public MetricSection section() {
        return this.section;
    }

    public String mergeField() {
        return this.mergeField;
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

    public String[] identityLabels() {
        return switch (this) {
            case HOST, ZFS_ARC -> new String[0];
            case CPU_CORE -> new String[]{"cpu"};
            case DISK, DISK_ETA_UNTIL_FULL -> new String[]{"disk"};
            case NETWORK -> new String[]{"interface"};
            case GPU -> new String[]{"gpu", "device_id", "vendor"};
            case DOCKER -> new String[]{"container"};
            case TEMPERATURE -> new String[]{"sensor"};
            case ZFS_POOL -> new String[]{"pool", "health", "scan_state"};
            case TCP -> new String[]{"state"};
        };
    }

    private static String diskEtaUntilFullPromql(long serverId) {
        String selector = "{server_id=\"" + serverId + "\"}";
        String total = "monitor_disk_total_bytes" + selector;
        String used = "monitor_disk_used_bytes" + selector;
        return "(" + total + " - " + used + ") / clamp_min((" + used + " - " + used + " offset 1d) / 86400, 1)";
    }
}
