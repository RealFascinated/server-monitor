package cc.fascinated.monitor.model.dto.request.server.ingest;

import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;

import java.util.List;

public record IngestServerMetrics(
        String agentVersion,
        ServerDetails serverDetails,
        ServerMetrics serverMetrics,
        List<InterfaceMetrics> interfaceMetrics,
        List<DiskMetric> diskMetrics
) {}
