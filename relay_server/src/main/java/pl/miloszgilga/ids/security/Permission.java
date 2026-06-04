package pl.miloszgilga.ids.security;

public enum Permission implements BitmaskPermission {
    ADMIN(0, true), // 1
    ENV_STATUS_VIEWER(1) // 2
    ;

    private final int shift;
    private final long bit;
    private final boolean isAdmin;

    Permission(int shift, boolean isAdmin) {
        this.shift = shift;
        this.bit = 1L << shift;
        this.isAdmin = isAdmin;
    }

    Permission(int shift) {
        this(shift, false);
    }

    @Override
    public int getShift() {
        return shift;
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
