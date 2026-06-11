package cc.fascinated.monitor.websocket;

import cc.fascinated.monitor.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import cc.fascinated.monitor.websocket.impl.ServersWebSocket;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketManager implements WebSocketConfigurer {

    private final ServersWebSocket serversWebSocket;
    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketManager(
            ServersWebSocket serversWebSocket,
            WebSocketAuthHandshakeInterceptor authHandshakeInterceptor
    ) {
        this.serversWebSocket = serversWebSocket;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        this.serversWebSocket.setObjectMapper(Constants.OBJECT_MAPPER);
        registry.addHandler(this.serversWebSocket, this.serversWebSocket.getPath())
                .addInterceptors(this.authHandshakeInterceptor)
                .setAllowedOrigins("*");
        log.info("Registered WebSocket at path {}", this.serversWebSocket.getPath());
    }
}
