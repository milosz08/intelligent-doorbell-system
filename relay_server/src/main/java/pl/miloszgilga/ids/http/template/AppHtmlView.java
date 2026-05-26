package pl.miloszgilga.ids.http.template;

public enum AppHtmlView implements HtmlView {
    DASHBOARD("template/dashboard.html"),
    LOGIN("template/login.html"),
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
