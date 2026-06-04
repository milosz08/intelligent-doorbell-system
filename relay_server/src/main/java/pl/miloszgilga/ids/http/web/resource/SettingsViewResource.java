package pl.miloszgilga.ids.http.web.resource;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;
import pl.miloszgilga.ids.http.web.auth.WebAuthenticated;
import pl.miloszgilga.ids.http.web.nav.NavRootPage;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;

@WebAuthenticated
@Path("/settings")
public class SettingsViewResource extends WebViewResourceBase {
    public SettingsViewResource(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager) {
        super(htmlTemplateEngine, navigationManager);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getSettings(@Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.SETTINGS, data, NavRootPage.SETTINGS))
                .cookie(killCookie)
                .build();
    }
}
