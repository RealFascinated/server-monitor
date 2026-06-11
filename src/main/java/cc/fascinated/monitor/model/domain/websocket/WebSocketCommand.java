package cc.fascinated.monitor.model.domain.websocket;

public enum WebSocketCommand {
    SERVER_UPDATE,
    SERVER_CREATED,
    SERVER_DELETED,
    SERVER_METRICS_UPDATE,
    MEMBER_CHANGE,
    INVITE_CREATED,
    INVITE_REVOKED
}
