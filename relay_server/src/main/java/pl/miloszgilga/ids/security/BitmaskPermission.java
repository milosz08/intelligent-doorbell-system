package pl.miloszgilga.ids.security;

public interface BitmaskPermission {
    int getShift();

    long getBit();

    default boolean isAdmin() {
        return false;
    }
}
