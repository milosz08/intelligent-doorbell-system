package pl.miloszgilga.ids.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionManagerTest {
    private final long ADMIN_BIT = TestPermission.ADMIN.getBit();
    private final long RING_BELL_BIT = TestPermission.RING_BELL.getBit();
    private final long OPEN_GATE_BIT = TestPermission.OPEN_GATE.getBit();

    private final PermissionManager<TestPermission> permissionManager = new PermissionManager<>(
            TestPermission.values());

    @Test
    @DisplayName("return true when user mask contains the exact required permission")
    void shouldReturnTrueWhenUserHasExactPermission() {
        final long userMask = RING_BELL_BIT;
        assertTrue(permissionManager.hasPermission(userMask, TestPermission.RING_BELL));
    }

    @Test
    @DisplayName("return false when user mask lacks the required permission")
    void shouldReturnFalseWhenUserLacksPermission() {
        final long userMask = RING_BELL_BIT;
        assertFalse(permissionManager.hasPermission(userMask, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("grant access automatically when user mask contains the admin bit")
    void shouldReturnTrueWhenUserIsAdmin() {
        final long userMask = ADMIN_BIT;
        assertTrue(permissionManager.hasPermission(userMask, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("return true when passing valid string permission and user has access")
    void shouldReturnTrueForValidStringPermission() {
        final long userMask = OPEN_GATE_BIT;
        assertTrue(permissionManager.hasPermission(userMask, "OPEN_GATE"));
        assertTrue(permissionManager.hasPermission(userMask, "open_gate"));
    }

    @Test
    @DisplayName("return false and handle exception when passing invalid string permission")
    void shouldReturnFalseForInvalidStringPermission() {
        final long userMask = ADMIN_BIT;
        assertFalse(permissionManager.hasPermission(userMask, "NON_EXISTING_ROLE"));
    }

    @Test
    @DisplayName("return true when user has at least one of the required permissions")
    void shouldReturnTrueWhenUserHasAtLeastOneRequiredPermission() {
        final long userMask = RING_BELL_BIT;
        assertTrue(permissionManager.hasAnyPermission(userMask, TestPermission.RING_BELL, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("return false when user has none of the required permissions")
    void shouldReturnFalseWhenUserHasNoneOfRequiredPermissions() {
        final long userMask = 0L;
        assertFalse(permissionManager.hasAnyPermission(userMask, TestPermission.RING_BELL, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("return true when user has all of the required permissions")
    void shouldReturnTrueWhenUserHasAllRequiredPermissions() {
        final long userMask = RING_BELL_BIT | OPEN_GATE_BIT;
        assertTrue(permissionManager.hasAllPermissions(userMask, TestPermission.RING_BELL, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("return false when user is missing at least one of the required permissions")
    void shouldReturnFalseWhenUserIsMissingAtLeastOnePermission() {
        final long userMask = RING_BELL_BIT;
        assertFalse(permissionManager.hasAllPermissions(userMask, TestPermission.RING_BELL, TestPermission.OPEN_GATE));
    }

    @Test
    @DisplayName("grant new permission by adding its bit to the current mask")
    void shouldAddNewPermissionToMask() {
        final long initialMask = RING_BELL_BIT;
        final long expectedMask = RING_BELL_BIT | OPEN_GATE_BIT;
        final long resultMask = permissionManager.grant(initialMask, TestPermission.OPEN_GATE);
        assertEquals(expectedMask, resultMask);
    }

    @Test
    @DisplayName("revoke permission by removing its bit from the current mask")
    void shouldRemovePermissionFromMask() {
        final long initialMask = RING_BELL_BIT | OPEN_GATE_BIT;
        final long expectedMask = RING_BELL_BIT;
        final long resultMask = permissionManager.revoke(initialMask, TestPermission.OPEN_GATE);
        assertEquals(expectedMask, resultMask);
    }

    @Test
    @DisplayName("convert bitmask into a list of active permission string names")
    void shouldReturnCorrectListOfActivePermissionsAsStrings() {
        final long userMask = ADMIN_BIT | OPEN_GATE_BIT;
        final List<String> activePermissions = permissionManager.getActivePermissionsAsStrings(userMask);
        assertEquals(2, activePermissions.size());
        assertTrue(activePermissions.contains("ADMIN"));
        assertTrue(activePermissions.contains("OPEN_GATE"));
        assertFalse(activePermissions.contains("RING_BELL"));
    }

    @Test
    @DisplayName("calculate mask correctly using the bitmask permission interface directly")
    void shouldWorkWithBitmaskPermissionInterfacePolymorphism() {
        final BitmaskPermission adminRole = TestPermission.ADMIN;
        final BitmaskPermission bellPerm = TestPermission.RING_BELL;
        final long newMask = adminRole.getBit() | bellPerm.getBit();
        assertEquals(3L, newMask);
    }
}
