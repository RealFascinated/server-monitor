package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record ServerDetails(
        String ip,
        int coreCount,
        int threadCount,
        String osName,
        String osVersion,
        long uptimeSeconds
) {}
