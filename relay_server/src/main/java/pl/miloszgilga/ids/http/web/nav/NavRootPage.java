package pl.miloszgilga.ids.http.web.nav;

import pl.miloszgilga.ids.security.Permission;

public enum NavRootPage {
    DASHBOARD("", "Dashboard"),
    EVENT_LOGS("event-logs", "Event logs"),
    USERS_AND_PRIVILEGES("users", "Users and privileges"),
    SETTINGS("settings", "Settings"),
    ;

    private final String path;
    private final String title;
    private final Permission[] permissions;

    NavRootPage(String path, String title, Permission... permissions) {
        this.path = path;
        this.title = title;
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public Permission[] getPermissions() {
        return permissions;
    }
}
