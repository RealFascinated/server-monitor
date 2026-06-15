package cc.fascinated.monitor.model.dto.response.agent;

import java.util.List;

public record AgentUpdateResponse(
        String version,
        List<AgentUpdateAssetResponse> assets
) {}
