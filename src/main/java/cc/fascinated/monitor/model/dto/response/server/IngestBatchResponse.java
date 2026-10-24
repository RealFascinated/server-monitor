package cc.fascinated.monitor.model.dto.response.server;

import java.util.Map;

/**
 * @param accepted number of samples written
 * @param skipped  number of samples skipped, by reason
 */
public record IngestBatchResponse(int accepted, Map<String, Integer> skipped) {}
