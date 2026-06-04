package pl.miloszgilga.ids.http.web.resource.user;

public record PermissionsModel(String name, String bitmask, long rawValue, boolean isAdmin, int countOfUsers)
        implements Comparable<PermissionsModel> {
    @Override
    public int compareTo(PermissionsModel other) {
        return Long.compare(rawValue, other.rawValue());
    }
}
