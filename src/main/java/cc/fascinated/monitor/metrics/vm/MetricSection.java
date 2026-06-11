package cc.fascinated.monitor.metrics.vm;

public enum MetricSection {
    HOST("host", true),
    CPU_CORES("cpuCores", false),
    DISKS("disks", false),
    NETWORKS("networks", false),
    GPUS("gpus", false),
    CONTAINERS("containers", false),
    TEMPERATURES("temperatures", false),
    ZFS_ARC("zfsArc", true),
    ZFS_POOLS("zfsPools", false),
    TCP_CONNECTIONS("tcpConnections", false);

    private final String jsonKey;
    private final boolean scalar;

    MetricSection(String jsonKey, boolean scalar) {
        this.jsonKey = jsonKey;
        this.scalar = scalar;
    }

    public String jsonKey() {
        return this.jsonKey;
    }

    public boolean scalar() {
        return this.scalar;
    }
}
