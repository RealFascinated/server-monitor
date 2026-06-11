package cc.fascinated.monitor.model.dto.websocket;

import cc.fascinated.monitor.model.dto.response.server.access.PendingInviteResponse;

public record InviteCreatedOwnerData(long serverId, PendingInviteResponse invite) {}
