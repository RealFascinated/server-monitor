package cc.fascinated.monitor.websocket.impl;

import cc.fascinated.monitor.service.ServerWebSocketService;
import cc.fascinated.monitor.websocket.WebSocket;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ServersWebSocket extends WebSocket {

    private final ServerWebSocketService serverWebSocketService;

    public ServersWebSocket(ServerWebSocketService serverWebSocketService) {
        super("/v1/ws/servers");
        this.serverWebSocketService = serverWebSocketService;
    }

    @Override
    public void onSessionConnect(WebSocketSession session) {
        long userId = ServerWebSocketService.userIdFromSession(session);
        this.serverWebSocketService.registerSession(userId, session);
    }

    @Override
    public void onSessionDisconnect(WebSocketSession session) {
        long userId = ServerWebSocketService.userIdFromSession(session);
        this.serverWebSocketService.unregisterSession(userId, session);
    }
}
