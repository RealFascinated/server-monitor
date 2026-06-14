package cc.fascinated.monitor.model.domain.server;

public enum ServerPermission {
    VIEW_SERVER,
    VIEW_METRICS,
    LIST_MEMBERS,
    LIST_INVITES,
    INVITE_MEMBERS,
    REMOVE_MEMBERS,
    REVOKE_INVITES,
    LEAVE_SERVER,
    RENAME_SERVER,
    DELETE_SERVER,
    ROTATE_INGEST_TOKEN,
    ASSIGN_FOLDER;

    public int bit() {
        return 1 << ordinal();
    }
}
