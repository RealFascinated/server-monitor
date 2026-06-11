package cc.fascinated.monitor.model.dto.websocket;

import cc.fascinated.monitor.model.dto.response.server.access.ServerAccessListResponse;

public record MemberChangeData(long serverId, ServerAccessListResponse access) {}
