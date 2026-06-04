package pl.miloszgilga.ids.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionManager<T extends Enum<T> & BitmaskPermission> {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionManager.class);

    private final Map<String, T> permissionMap;
    private final long adminMask;

    public PermissionManager(T[] permissions) {
        permissionMap = Arrays.stream(permissions)
                .collect(Collectors.toUnmodifiableMap(
                        p -> ((Enum<?>) p).name().toUpperCase(),
                        Function.identity()));
        adminMask = Arrays.stream(permissions)
                .filter(BitmaskPermission::isAdmin)
                .mapToLong(BitmaskPermission::getBit)
                .reduce(0L, (acc, bit) -> acc | bit);
    }

    public boolean isUserAdmin(long mask) {
        return (mask & adminMask) != 0;
    }

    public long generateMask(List<String> permissionNames) {
        if (permissionNames == null || permissionNames.size() == 0) {
            return 0L;
        }
        long generatedMask = 0L;
        for (final String name : permissionNames) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            final T permission = permissionMap.get(name.toUpperCase());
            if (permission != null) {
                generatedMask |= permission.getBit();
            } else {
                LOG.warn("Skipping unknown permission name '{}' during mask generation", name);
            }
        }
        LOG.debug("Generated mask: {} for requested permissions: {}", generatedMask, permissionNames);
        return generatedMask;
    }

    public boolean hasPermission(long userMask, BitmaskPermission requiredPermission, boolean overrideForAdmin) {
        if (requiredPermission == null) {
            return false;
        }
        if (isUserAdmin(userMask) && overrideForAdmin) {
            LOG.debug("Access granted: user has ADMIN override");
            return true;
        }
        final boolean hasPerm = (userMask & requiredPermission.getBit()) != 0;
        LOG.debug("Permission '{}' evaluation result: {}", requiredPermission, hasPerm);
        return hasPerm;
    }

    public boolean hasPermission(long userMask, BitmaskPermission requiredPermission) {
        return hasPermission(userMask, requiredPermission, true);
    }

    public boolean hasPermission(long userMask, String requiredRoleOrPermission, boolean overrideForAdmin) {
        try {
            final T permission = permissionMap.get(requiredRoleOrPermission.toUpperCase());
            return hasPermission(userMask, permission, overrideForAdmin);
        } catch (IllegalArgumentException ex) {
            LOG.error("Unable to find followed permission: {}, cause: {}", requiredRoleOrPermission,
                    ex.getMessage());
            return false;
        }
    }

    public boolean hasPermission(long userMask, String requiredRoleOrPermission) {
        return hasPermission(userMask, requiredRoleOrPermission, true);
    }

    public boolean hasAnyPermission(long userMask, BitmaskPermission... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }
        if (isUserAdmin(userMask)) {
            LOG.debug("Access granted: user has ADMIN override");
            return true;
        }
        long requiredMask = 0L;
        for (final BitmaskPermission permission : requiredPermissions) {
            if (permission != null) {
                requiredMask |= permission.getBit();
            }
        }
        final boolean hasPerm = (userMask & requiredMask) != 0;
        LOG.debug("ANY permission evaluation result: {}", hasPerm);
        return hasPerm;
    }

    public boolean hasAllPermissions(long userMask, BitmaskPermission... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }
        if (isUserAdmin(userMask)) {
            return true;
        }
        long requiredMask = 0L;
        for (final BitmaskPermission permission : requiredPermissions) {
            requiredMask |= permission.getBit();
        }
        final boolean hasPerm = (userMask & requiredMask) == requiredMask;
        LOG.debug("ALL permissions evaluation result: {}", hasPerm);
        return hasPerm;
    }

    public long grant(long currentMask, BitmaskPermission permissionToAdd) {
        final long newMask = currentMask | permissionToAdd.getBit();
        LOG.debug("Granted '{}', old mask: {}, new mask: {}", permissionToAdd, currentMask, newMask);
        return newMask;
    }

    public long revoke(long currentMask, BitmaskPermission permissionToRemove) {
        final long newMask = currentMask & ~permissionToRemove.getBit();
        LOG.debug("Revoked '{}', old mask: {}, new mask: {}", permissionToRemove, currentMask, newMask);
        return newMask;
    }

    public List<String> getActivePermissionsAsStrings(long userMask) {
        return permissionMap.entrySet().stream()
                .filter(entry -> (userMask & entry.getValue().getBit()) != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int countEntitiesWithPermission(List<? extends MaskBearer> entities, BitmaskPermission permission) {
        if (entities == null || permission == null) {
            return 0;
        }
        return (int) entities.stream()
                .filter(entity -> (entity.getPermissionsMask() & permission.getBit()) != 0)
                .count();
    }

    public String formatBitmaskString(BitmaskPermission permission) {
        if (permission == null || permission.getBit() == 0) {
            return "0";
        }
        final int shift = Long.numberOfTrailingZeros(permission.getBit());
        return "1 << " + shift;
    }

    public Collection<T> getPermissions() {
        return permissionMap.values();
    }
}
