package cc.fascinated.monitor.model.dto.response.server;

public record ServerMemorySnapshot(
        Long usage,
        Long max
) {}
