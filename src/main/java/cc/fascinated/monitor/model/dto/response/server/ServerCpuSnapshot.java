package cc.fascinated.monitor.model.dto.response.server;

public record ServerCpuSnapshot(
        Double percent,
        Double user,
        Double system,
        Double iowait,
        Double steal
) {}
