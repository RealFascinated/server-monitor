package cc.fascinated.monitor.model.dto.request.server.ingest;

import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsArcMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsPoolMetric;

import java.util.List;

public record IngestServerMetrics(
        String agentVersion,
        ServerDetails serverDetails,
        ServerMetrics serverMetrics,
        ZfsArcMetrics zfsArcMetrics,
        List<ZfsPoolMetric> zfsPoolMetrics,
        List<InterfaceMetrics> interfaceMetrics,
        List<DiskMetric> diskMetrics
) {}
