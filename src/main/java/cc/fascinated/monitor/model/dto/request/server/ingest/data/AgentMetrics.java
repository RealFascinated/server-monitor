package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.Min;

/**
 * Resource usage of the agent process itself, reported so the fleet's agent health and push problems are visible to admins.
 */
public record AgentMetrics(
        @Min(0) Long rssBytes,
        @Min(0) Long goroutines,
        @Min(0) Long heapInUseBytes,
        @Min(0) Long openFds,
        @Min(0) Long uptimeSeconds,
        @Min(0) Long lastPushDurationMs,
        @Min(0) Long lastPushPayloadBytes,
        @Min(0) Long pushFailuresTotal,
        @Min(0) Long bufferedSamples,
        @Min(0) Long bufferedBytes,
        @Min(0) Long bufferOldestAgeSeconds,
        @Min(0) Long bufferDroppedTotal
) {}
