package cc.fascinated.monitor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Slf4j
public abstract class WebSocket extends TextWebSocketHandler {

    public final String path;

    private final List<WebSocketSession> sessions = new ArrayList<>();
    @Setter
    private ObjectMapper objectMapper;

    @SneakyThrows
    public void sendMessage(WebSocketSession session, Object message) {
        if (!session.isOpen()) {
            return;
        }
        String payload = message instanceof String text ? text : this.objectMapper.writeValueAsString(message);
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(payload));
        }
    }

    public void sendMessageToAll(Object message) {
        for (WebSocketSession session : this.sessions) {
            this.sendMessage(session, message);
        }
    }

    public void onSessionConnect(WebSocketSession session) {}

    public void onSessionDisconnect(WebSocketSession session) {}

    @Override
    public final void afterConnectionEstablished(@NotNull WebSocketSession session) {
        this.sessions.add(session);
        log.info("Connection established on {} ({})", this.getPath(), session.getId());
        this.onSessionConnect(session);
    }

    @Override
    public final void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        this.sessions.remove(session);
        log.info("Connection closed on {} ({})", this.getPath(), session.getId());
        this.onSessionDisconnect(session);
    }
}
