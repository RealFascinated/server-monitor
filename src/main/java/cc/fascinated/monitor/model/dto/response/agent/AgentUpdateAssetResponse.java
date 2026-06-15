package cc.fascinated.monitor.model.dto.response.agent;

public record AgentUpdateAssetResponse(
        String name,
        String downloadUrl,
        String checksum
) {}
