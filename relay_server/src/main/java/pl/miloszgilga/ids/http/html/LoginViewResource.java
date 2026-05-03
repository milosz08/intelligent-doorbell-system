package pl.miloszgilga.ids.http.html;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.TemplateEngine;

@PublicViewOnly
@Path("/login")
public class LoginViewResource {
    private final TemplateEngine templateEngine;
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final PasswordManager passwordManager;
    private final int sessionTtlSec;

    public LoginViewResource(TemplateEngine templateEngine, UserDao userDao, SessionDao sessionDao,
            PasswordManager passwordManager, int sessionTtlSec) {
        this.templateEngine = templateEngine;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.passwordManager = passwordManager;
        this.sessionTtlSec = sessionTtlSec;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin() {
        return Response.ok(templateEngine.parseTemplate(AppHtmlView.LOGIN)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response postLogin(@FormParam("username") String username, @FormParam("password") String password) {
        final Map<String, Object> data = new HashMap<>();
        if (username == null || password == null || !passwordManager.verify(username, password)) {
            data.put("errorMessage", "Incorrect login and/or password");
            data.put("previousUsername", username);
            return Response.ok(templateEngine.parseTemplate(AppHtmlView.LOGIN, data)).build();
        }
        final String sessionId = UUID.randomUUID().toString();
        final Integer userId = userDao.getUserId(username);
        final Instant expiresAt = Instant.now().plusSeconds(sessionTtlSec);
        sessionDao.createSession(sessionId, userId, expiresAt);
        final NewCookie sessionCookie = new NewCookie.Builder("JSESSIONID")
                .value(sessionId)
                .path("/")
                .httpOnly(true)
                .maxAge(sessionTtlSec)
                .build();
        return Response.seeOther(URI.create("/"))
                .cookie(sessionCookie)
                .build();
    }
}
