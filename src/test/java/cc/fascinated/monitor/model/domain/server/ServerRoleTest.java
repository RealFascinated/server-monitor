package cc.fascinated.monitor.model.domain.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRoleTest {
    @Test
    void permission_bit_isPowerOfTwoFromOrdinal() {
        for (ServerPermission permission : ServerPermission.values()) {
            assertEquals(1 << permission.ordinal(), permission.bit());
        }
    }

    @Test
    void all_equalsUnionOfEveryPermissionBit() {
        int expected = 0;
        for (ServerPermission permission : ServerPermission.values()) {
            expected |= permission.bit();
        }
        assertEquals(expected, ServerPermissions.ALL);
    }

    @Test
    void permissionMask_matchesHasChecks() {
        assertEquals(ServerPermissions.ALL & ~ServerPermission.LEAVE_SERVER.bit(), ServerRole.OWNER.permissionMask());
        assertEquals(
                ServerPermissions.maskOf(
                        ServerPermission.VIEW_SERVER,
                        ServerPermission.VIEW_METRICS,
                        ServerPermission.LEAVE_SERVER,
                        ServerPermission.ASSIGN_FOLDER
                ),
                ServerRole.VIEWER.permissionMask()
        );
    }

    @Test
    void owner_hasAllPermissionsExceptLeaveServer() {
        for (ServerPermission permission : ServerPermission.values()) {
            if (permission == ServerPermission.LEAVE_SERVER) {
                assertFalse(ServerRole.OWNER.has(permission));
            } else {
                assertTrue(ServerRole.OWNER.has(permission));
            }
        }
        assertEquals(
                ServerPermissions.ALL & ~ServerPermission.LEAVE_SERVER.bit(),
                ServerPermissions.maskOf(
                        ServerPermission.VIEW_SERVER,
                        ServerPermission.VIEW_METRICS,
                        ServerPermission.LIST_MEMBERS,
                        ServerPermission.LIST_INVITES,
                        ServerPermission.INVITE_MEMBERS,
                        ServerPermission.REMOVE_MEMBERS,
                        ServerPermission.REVOKE_INVITES,
                        ServerPermission.RENAME_SERVER,
                        ServerPermission.DELETE_SERVER,
                        ServerPermission.ROTATE_INGEST_TOKEN,
                        ServerPermission.ASSIGN_FOLDER
                )
        );
    }

    @Test
    void viewer_hasOnlyExpectedPermissions() {
        assertTrue(ServerRole.VIEWER.has(ServerPermission.VIEW_SERVER));
        assertTrue(ServerRole.VIEWER.has(ServerPermission.VIEW_METRICS));
        assertTrue(ServerRole.VIEWER.has(ServerPermission.LEAVE_SERVER));
        assertTrue(ServerRole.VIEWER.has(ServerPermission.ASSIGN_FOLDER));

        assertFalse(ServerRole.VIEWER.has(ServerPermission.LIST_MEMBERS));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.LIST_INVITES));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.INVITE_MEMBERS));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.REMOVE_MEMBERS));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.REVOKE_INVITES));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.RENAME_SERVER));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.DELETE_SERVER));
        assertFalse(ServerRole.VIEWER.has(ServerPermission.ROTATE_INGEST_TOKEN));
    }
}
