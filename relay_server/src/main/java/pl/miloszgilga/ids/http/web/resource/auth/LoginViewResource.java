package pl.miloszgilga.ids.http.web.resource.auth;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.http.Constants;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.HttpWebPipelineException;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;
import pl.miloszgilga.ids.http.web.auth.PublicViewOnly;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;
import pl.miloszgilga.ids.http.web.resource.WebViewResourceBase;

@PublicViewOnly
@Path("/login")
public class LoginViewResource extends WebViewResourceBase {
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final PasswordManager passwordManager;
    private final int sessionTtlSec;

    public LoginViewResource(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager,
            UserDao userDao, SessionDao sessionDao, PasswordManager passwordManager, int sessionTtlSec) {
        super(htmlTemplateEngine, navigationManager);
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.passwordManager = passwordManager;
        this.sessionTtlSec = sessionTtlSec;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin(@Context HttpServletRequest req) {
        final Map<String, Object> data = new HashMap<>();
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(AppHtmlView.LOGIN, data))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response postLogin(@FormParam("username") String username, @FormParam("password") String password,
            @Context HttpServletRequest req) {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, String> formErrors = new HashMap<>();
        try {
            if (Utils.isNullOrBlank(username)) {
                formErrors.put("username", "Username/login must be set");
            }
            if (Utils.isNullOrBlank(password)) {
                formErrors.put("password", "Password must be set");
            }
            if (formErrors.isEmpty()) {
                if (!passwordManager.verify(username, password)) {
                    throw new HttpWebPipelineException("Incorrect login and/or password");
                }
                final String sessionId = UUID.randomUUID().toString();
                final Long userId = userDao.getUserId(username);
                if (userId == null) {
                    throw new HttpWebPipelineException("Unable to find user with username '%s'", username);
                }
                final Instant expiresAt = Instant.now().plusSeconds(sessionTtlSec);
                if (!sessionDao.createSession(sessionId, userId, expiresAt)) {
                    throw new HttpWebPipelineException("Uknown error during created new session");
                }
                final boolean hasDefaultPassword = userDao.userHasDefaultPassword(username);
                String redirectUrl = "/";
                if (hasDefaultPassword) {
                    redirectUrl = "/change-password";
                }
                final NewCookie sessionCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                        .value(sessionId)
                        .path("/")
                        .httpOnly(true)
                        .maxAge(sessionTtlSec)
                        .build();
                return Response.seeOther(URI.create(redirectUrl))
                        .cookie(sessionCookie,
                                FlashAlertManager.setSuccess(req, "Successfully logged on '%s' user account",
                                        username))
                        .build();
            }
        } catch (HttpWebPipelineException ex) {
            data.put("username", username);
            FlashAlertManager.setDanger(req, ex.getMessage());
        } finally {
            data.put("formErrors", formErrors);
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(AppHtmlView.LOGIN, data))
                .cookie(killCookie)
                .build();
    }
}
