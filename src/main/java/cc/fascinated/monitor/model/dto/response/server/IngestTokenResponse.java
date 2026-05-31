package cc.fascinated.monitor.model.dto.response.server;

import java.util.UUID;

public record IngestTokenResponse(
        Long serverId,
        UUID ingestToken
) {}
