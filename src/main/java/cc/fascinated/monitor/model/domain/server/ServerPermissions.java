package cc.fascinated.monitor.model.domain.server;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServerPermissions {
    public static final int ALL = maskOf(ServerPermission.values());

    public static int maskOf(ServerPermission... permissions) {
        int mask = 0;
        for (ServerPermission permission : permissions) {
            mask |= permission.bit();
        }
        return mask;
    }
}
