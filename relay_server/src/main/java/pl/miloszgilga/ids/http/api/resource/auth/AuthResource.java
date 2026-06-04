package pl.miloszgilga.ids.http.api.resource.auth;

import java.time.Instant;
import java.util.UUID;

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
import pl.miloszgilga.ids.http.api.HttpApiPipelineException;
import pl.miloszgilga.ids.http.api.auth.ApiAuthenticated;
import pl.miloszgilga.ids.http.api.resource.ApiResourceBase;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource extends ApiResourceBase {
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
        try {
            final String username = request.username();
            final String password = request.password();
            if (username == null || password == null || !passwordManager.verify(username, password)) {
                throw new HttpApiPipelineException(Response.Status.UNAUTHORIZED);
            }
            final Long userId = userDao.getUserId(username);
            if (userId == null) {
                throw new HttpApiPipelineException(Response.Status.UNAUTHORIZED);
            }
            final String sessionToken = UUID.randomUUID().toString();
            final Instant expiresAt = Instant.now().plusSeconds(sessionTtlSec);
            if (!sessionDao.createSession(sessionToken, userId, expiresAt)) {
                throw new HttpApiPipelineException(Response.Status.INTERNAL_SERVER_ERROR);
            }
            return Response.ok(new LoginResponse(Constants.SID_HEADER_NAME, sessionToken)).build();
        } catch (HttpApiPipelineException ex) {
            return generateGenericError(ex);
        }
    }

    @ApiAuthenticated
    @DELETE
    @Path("/logout")
    public Response logout(@Context ContainerRequestContext crc) {
        final String sessionId = (String) crc.getProperty("sessionId");
        try {
            if (sessionId != null) {
                throw new HttpApiPipelineException(Response.Status.NOT_FOUND);
            }
            if (!sessionDao.destroySession(sessionId)) {
                throw new HttpApiPipelineException(Response.Status.INTERNAL_SERVER_ERROR);
            }
            return Response.noContent().build();
        } catch (HttpApiPipelineException ex) {
            return generateGenericError(ex);
        }
    }
}
