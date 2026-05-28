package cc.fascinated.monitor.model.dto.response.server;

import java.util.UUID;

public record CreatedServerResponse(
        String serverName,
        Long serverId,
        UUID ingestToken
) {}
