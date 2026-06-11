package cc.fascinated.monitor.model.dto.websocket;

import cc.fascinated.monitor.model.dto.response.server.access.UserPendingInviteResponse;

public record InviteCreatedInviteeData(UserPendingInviteResponse invite) {}
