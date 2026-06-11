package cc.fascinated.monitor.metrics.vm.catalog;

import cc.fascinated.monitor.metrics.vm.MetricSection;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum MetricFamily {
    HOST(MetricSection.HOST, new String[0]),
    CPU_CORE(MetricSection.CPU_CORES, new String[]{"cpu"}),
    DISK(MetricSection.DISKS, new String[]{"disk"}),
    NETWORK(MetricSection.NETWORKS, new String[]{"interface"}),
    GPU(MetricSection.GPUS, new String[]{"device_id", "gpu", "vendor"}, new String[]{"device_id"}),
    DOCKER(MetricSection.CONTAINERS, new String[]{"container"}),
    TEMPERATURE(MetricSection.TEMPERATURES, new String[]{"sensor"}),
    ZFS_ARC(MetricSection.ZFS_ARC, new String[0]),
    ZFS_POOL(MetricSection.ZFS_POOLS, new String[]{"pool", "health", "scan_state"}),
    TCP(MetricSection.TCP_CONNECTIONS, new String[]{"state"});

    private final MetricSection section;
    private final String[] identityLabels;
    private final String[] groupingLabels;

    MetricFamily(MetricSection section, String[] identityLabels) {
        this(section, identityLabels, identityLabels);
    }

    MetricFamily(MetricSection section, String[] identityLabels, String[] groupingLabels) {
        this.section = section;
        this.identityLabels = identityLabels;
        this.groupingLabels = groupingLabels;
    }
}
