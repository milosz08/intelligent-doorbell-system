package pl.miloszgilga.ids.security;

public enum Permission implements BitmaskPermission {
    ADMIN(1L << 0, true), // 1
    ENV_STATUS_VIEWER(1L << 1) // 2
    ;

    private final long bit;
    private final boolean isAdmin;

    Permission(long bit, boolean isAdmin) {
        this.bit = bit;
        this.isAdmin = isAdmin;
    }

    Permission(long bit) {
        this(bit, false);
    }

    @Override
    public long getBit() {
        return bit;
    }

    @Override
    public boolean isAdmin() {
        return isAdmin;
    }
}
