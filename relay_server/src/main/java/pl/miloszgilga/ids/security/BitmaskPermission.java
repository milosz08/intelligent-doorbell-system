package pl.miloszgilga.ids.security;

public interface BitmaskPermission {
    long getBit();

    default boolean isAdmin() {
        return false;
    }
}
