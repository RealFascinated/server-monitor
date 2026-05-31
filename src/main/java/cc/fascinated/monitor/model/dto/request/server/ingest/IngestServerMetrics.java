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
        @Nullable List<GpuMetric> gpuMetrics,
        @Nullable List<ZfsPoolMetric> zfsPoolMetrics,
        @Nullable ZfsArcMetrics zfsArcMetrics,
        @Nullable List<DockerContainerMetric> dockerContainers
) {}
