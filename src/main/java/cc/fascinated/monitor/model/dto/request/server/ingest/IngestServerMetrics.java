package cc.fascinated.monitor.model.dto.request.server.ingest;

import cc.fascinated.monitor.model.dto.request.server.ingest.data.*;

import java.util.List;

public record IngestServerMetrics(
        String agentVersion,
        ServerDetails serverDetails,
        ServerMetrics serverMetrics,
        ZfsArcMetrics zfsArcMetrics,
        List<ZfsPoolMetric> zfsPoolMetrics,
        List<InterfaceMetrics> interfaceMetrics,
        List<DiskMetric> diskMetrics,
        List<DockerContainerMetric> dockerContainers
) {}
