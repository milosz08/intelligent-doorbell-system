package pl.miloszgilga.ids.http.template;

public enum AppHtmlView implements HtmlView {
    // auth
    CHANGE_PASSWORD("auth/change-password"),
    LOGIN("auth/login"),
    // user
    ADD_USER("user/add-user"),
    USER_PRIVILEGES("user/user-privileges"),
    USERS("user/users"),
    // others
    DASHBOARD("dashboard"),
    ERROR("error"),
    EVENT_LOGS("event-logs"),
    SETTINGS("settings"),
    ;

    private final String path;

    private AppHtmlView(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
