package pl.miloszgilga.ids.http.html;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.Constants;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.TemplateEngine;

@ViewAuthenticated
@Path("/")
public class DashboardViewResource {
    private final TemplateEngine templateEngine;
    private final UserDao userDao;
    private final SessionDao sessionDao;

    public DashboardViewResource(TemplateEngine templateEngine, UserDao userDao, SessionDao sessionDao) {
        this.templateEngine = templateEngine;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getDashboard(@Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        final UserDetails user = (UserDetails) crc.getProperty("authenticatedUser");
        data.put("user", user);
        // todo
        userDao.getUsers();
        return Response.ok(templateEngine.parseTemplate(AppHtmlView.DASHBOARD, data)).build();
    }

    @POST
    @Path("/logout")
    public Response postLogout(@Context ContainerRequestContext crc) {
        final String sessionId = (String) crc.getProperty("sessionId");
        if (sessionId != null) {
            sessionDao.destroySession(sessionId);
        }
        final NewCookie deleteCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                .value("")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .build();
        return Response.seeOther(URI.create("/login"))
                .cookie(deleteCookie)
                .build();
    }
}
