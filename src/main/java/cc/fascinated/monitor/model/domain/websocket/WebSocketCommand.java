package cc.fascinated.monitor.model.domain.websocket;

public enum WebSocketCommand {
    SERVERS_UPDATE,
    SERVER_CREATED,
    SERVER_DELETED,
    SERVER_METRICS_UPDATED,
    MEMBER_CHANGE,
    INVITE_CREATED,
    INVITE_REVOKED
}
