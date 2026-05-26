package pl.miloszgilga.ids.security;

public enum TestPermission implements BitmaskPermission {
    ADMIN(1L << 0, true), // 1
    RING_BELL(1L << 1), // 2
    OPEN_GATE(1L << 2), // 4
    READ_NOTIFICATIONS(1L << 3), // 8
    MANAGE_USERS(1L << 4), // 16
    ;

    private final long bit;
    private final boolean isAdmin;

    TestPermission(long bit, boolean isAdmin) {
        this.bit = bit;
        this.isAdmin = isAdmin;
    }

    TestPermission(long bit) {
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
