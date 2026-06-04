package pl.miloszgilga.ids.http.web.resource;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import pl.miloszgilga.ids.AppBuildConfig;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.template.HtmlView;
import pl.miloszgilga.ids.http.web.HttpWebPipelineException;
import pl.miloszgilga.ids.http.web.nav.NavRootPage;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;

public abstract class WebViewResourceBase {
    private final HtmlTemplateEngine htmlTemplateEngine;
    private final NavigationManager navigationManager;

    protected WebViewResourceBase(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager) {
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.navigationManager = navigationManager;
    }

    protected String getSessionId(ContainerRequestContext crc) {
        final String sessionId = (String) crc.getProperty("sessionId");
        if (sessionId == null) {
            throw new HttpWebPipelineException("Unable to find session");
        }
        return sessionId;
    }

    protected UserDetails getSafetyLoggedUser(ContainerRequestContext crc) {
        final UserDetails user = (UserDetails) crc.getProperty("authenticatedUser");
        if (user == null) {
            throw new HttpWebPipelineException("Unable to find logged user");
        }
        return user;
    }

    protected String parseTemplate(HtmlView htmlView, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("compilationHash", AppBuildConfig.COMPILATION_HASH);
        data.put("compilationLongHash", AppBuildConfig.COMPILATION_LONG_HASH);
        data.put("lastUpdatedUTC", AppBuildConfig.BUILD_TIME);
        return htmlTemplateEngine.parseTemplate(htmlView, data);
    }

    protected String parseTemplate(HtmlView htmlView) {
        return parseTemplate(htmlView, null);
    }

    protected String parseTemplate(ContainerRequestContext crc, HtmlView htmlView, Map<String, Object> data,
            NavRootPage activePage) {
        if (data == null) {
            data = new HashMap<>();
        }
        final UserDetails user = getSafetyLoggedUser(crc);
        data.put("loggedUser", getSafetyLoggedUser(crc));
        data.put("navLinks", navigationManager.buildNavigation(user, activePage));
        return parseTemplate(htmlView, data);
    }

    protected String parseTemplate(ContainerRequestContext crc, HtmlView htmlView, NavRootPage activePage) {
        return parseTemplate(crc, htmlView, null, activePage);
    }
}
