package pl.miloszgilga.ids.http.web.resource.user;

public record PermissionModel(String name, String bitmask, long rawValue, boolean isAdmin, boolean isActive)
        implements Comparable<PermissionModel> {
    @Override
    public int compareTo(PermissionModel other) {
        return Long.compare(rawValue, other.rawValue());
    }
}
