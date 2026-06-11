package cc.fascinated.monitor.model.dto.response.server.access;

import java.util.List;

public record ServerAccessListResponse(
        ServerAccessUserResponse owner,
        List<ServerMemberEntryResponse> members,
        List<PendingInviteResponse> pendingInvites
) {}
