package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record ZfsArcMetrics(
        long arcSizeBytes,
        long arcTargetBytes,
        long arcMaxBytes,
        long arcMinBytes,
        long arcDataBytes,
        long arcMetadataBytes,
        long l2arcSizeBytes,
        double arcHitRatio,
        long arcMissesPerSecond
) {}
