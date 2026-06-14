package cc.fascinated.monitor.model.domain.server;

public enum ServerRole {
    OWNER(ServerPermissions.ALL & ~ServerPermission.LEAVE_SERVER.bit()),
    VIEWER(
            ServerPermission.VIEW_SERVER,
            ServerPermission.VIEW_METRICS,
            ServerPermission.LEAVE_SERVER,
            ServerPermission.ASSIGN_FOLDER
    );

    private final int permissionMask;

    ServerRole(int permissionMask) {
        this.permissionMask = permissionMask;
    }

    ServerRole(ServerPermission... permissions) {
        this.permissionMask = ServerPermissions.maskOf(permissions);
    }

    public boolean has(ServerPermission permission) {
        return (permissionMask & permission.bit()) != 0;
    }

    public int permissionMask() {
        return permissionMask;
    }
}
