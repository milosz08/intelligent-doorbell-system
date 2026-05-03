package pl.miloszgilga.ids.http.template;

public enum AppHtmlView implements HtmlView {
    DASHBOARD("template/dashboard.vm"),
    LOGIN("template/login.vm"),
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
