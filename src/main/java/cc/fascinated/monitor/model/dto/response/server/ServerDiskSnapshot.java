package cc.fascinated.monitor.model.dto.response.server;

public record ServerDiskSnapshot(
        Long usage,
        Long max
) {}
