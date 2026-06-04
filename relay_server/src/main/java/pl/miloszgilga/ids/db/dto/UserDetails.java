package pl.miloszgilga.ids.db.dto;

import pl.miloszgilga.ids.security.MaskBearer;

public record UserDetails(long id, String username, boolean isActive, long permissionsMask, boolean isSystemAccount)
        implements MaskBearer {
    @Override
    public long getPermissionsMask() {
        return permissionsMask;
    }
}
