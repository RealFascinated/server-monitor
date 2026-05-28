package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record InterfaceMetrics(
        String interfaceName,
        long rxBytesPerSecond,
        long txBytesPerSecond,
        long rxPacketsPerSecond,
        long txPacketsPerSecond,
        long rxErrorsPerSecond,
        long txErrorsPerSecond
) {}
