package cc.fascinated.monitor.model.dto.websocket;

import java.util.List;

public record ServerMetricsUpdatedData(List<Long> serverIds) {}
