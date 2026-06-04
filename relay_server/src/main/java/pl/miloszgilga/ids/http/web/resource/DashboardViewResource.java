package pl.miloszgilga.ids.http.web.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.http.Constants;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.HttpWebPipelineException;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;
import pl.miloszgilga.ids.http.web.auth.WebAuthenticated;
import pl.miloszgilga.ids.http.web.nav.NavRootPage;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;
import pl.miloszgilga.ids.net.NetworkProvider;

@WebAuthenticated
@Path("/")
public class DashboardViewResource extends WebViewResourceBase {
    private final SessionDao sessionDao;
    private final NetworkProvider networkProvider;
    private final String mdnsServiceName;

    public DashboardViewResource(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager,
            SessionDao sessionDao, NetworkProvider networkProvider, String mdnsServiceName) {
        super(htmlTemplateEngine, navigationManager);
        this.networkProvider = networkProvider;
        this.mdnsServiceName = mdnsServiceName;
        this.sessionDao = sessionDao;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getDashboard(@Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();

        data.put("serverIp", networkProvider.getLanInetAddress().getHostAddress());
        data.put("mdnsServiceName", mdnsServiceName);

        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.DASHBOARD, data, NavRootPage.DASHBOARD))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.TEXT_HTML)
    public Response postLogout(@Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        try {
            final String sessionId = getSessionId(crc);
            if (!sessionDao.destroySession(sessionId)) {
                throw new HttpWebPipelineException("Unable to destroy session");
            }
            final NewCookie deleteCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                    .value("")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .build();
            return Response.seeOther(URI.create("/login"))
                    .cookie(deleteCookie,
                            FlashAlertManager.setSuccess(req, "Successfully logout from session"))
                    .build();
        } catch (HttpWebPipelineException ex) {
            FlashAlertManager.setDanger(req, ex.getMessage());
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.DASHBOARD, data, NavRootPage.DASHBOARD))
                .cookie(killCookie)
                .build();
    }
}
