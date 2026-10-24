package cc.fascinated.monitor.model.dto.request.server.ingest;

import java.util.List;

/**
 * Samples an agent buffered while it could not reach the API.
 *
 * @param sentAt  the agent's clock (unix seconds) when it sent the batch, used to correct clock skew
 * @param samples the buffered samples, each carrying the agent-clock {@code timestamp} it was taken at
 */
public record IngestBatchRequest(Long sentAt, List<IngestServerMetrics> samples) {}
