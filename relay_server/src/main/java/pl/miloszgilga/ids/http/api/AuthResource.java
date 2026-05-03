package pl.miloszgilga.ids.http.api;

import java.time.Instant;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.http.Constants;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private final PasswordManager passwordManager;
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final int sessionTtlSec;

    public AuthResource(PasswordManager passwordManager, UserDao userDao, SessionDao sessionDao, int sessionTtlSec) {
        this.passwordManager = passwordManager;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.sessionTtlSec = sessionTtlSec;
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        final String username = request.username();
        final String password = request.password();
        if (username == null || password == null || !passwordManager.verify(username, password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        final Integer userId = userDao.getUserId(username);
        final String sessionToken = java.util.UUID.randomUUID().toString();
        final Instant expiresAt = Instant.now().plusSeconds(sessionTtlSec);
        sessionDao.createSession(sessionToken, userId, expiresAt);
        return Response.ok(new LoginResponse(Constants.SID_HEADER_NAME, sessionToken)).build();
    }

    @Authenticated
    @DELETE
    @Path("/logout")
    public Response logout(@Context ContainerRequestContext crc) {
        final String sessionId = (String) crc.getProperty("sessionId");
        if (sessionId != null) {
            sessionDao.destroySession(sessionId);
        }
        return Response.noContent().build();
    }
}
