package cc.fascinated.monitor.model.dto.websocket;

import cc.fascinated.monitor.model.dto.response.server.ServerResponse;

import java.util.List;

public record ServersUpdateData(List<ServerResponse> servers) {}
