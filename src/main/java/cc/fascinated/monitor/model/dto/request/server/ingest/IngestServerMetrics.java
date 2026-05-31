package cc.fascinated.monitor.model.dto.request.server.ingest;

import cc.fascinated.monitor.model.dto.request.server.ingest.data.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record IngestServerMetrics(
        String agentVersion,
        ServerDetails serverDetails,
        ServerMetrics serverMetrics,
        List<InterfaceMetrics> interfaceMetrics,
        List<DiskMetric> diskMetrics,
        @Nullable List<ZfsPoolMetric> zfsPoolMetrics,
        @Nullable ZfsArcMetrics zfsArcMetrics,
        @Nullable List<DockerContainerMetric> dockerContainers,
        @Nullable List<CpuCoreMetric> cpuCoreMetrics,
        @Nullable List<TemperatureMetric> temperatureMetrics
) {
    public List<CpuCoreMetric> resolvedCpuCoreMetrics() {
        if (this.cpuCoreMetrics != null) {
            return this.cpuCoreMetrics;
        }
        return this.serverMetrics != null ? this.serverMetrics.cpuCoreMetrics() : null;
    }

    public List<TemperatureMetric> resolvedTemperatureMetrics() {
        if (this.temperatureMetrics != null) {
            return this.temperatureMetrics;
        }
        return this.serverMetrics != null ? this.serverMetrics.temperatureMetrics() : null;
    }
}
