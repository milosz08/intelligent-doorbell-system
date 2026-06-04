package pl.miloszgilga.ids.security;

public enum TestPermission implements BitmaskPermission {
    ADMIN(0, true), // 1
    RING_BELL(1), // 2
    OPEN_GATE(2), // 4
    READ_NOTIFICATIONS(3), // 8
    MANAGE_USERS(4), // 16
    ;

    private final int shift;
    private final long bit;
    private final boolean isAdmin;

    TestPermission(int shift, boolean isAdmin) {
        this.shift = shift;
        this.bit = 1L << shift;
        this.isAdmin = isAdmin;
    }

    TestPermission(int shift) {
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
