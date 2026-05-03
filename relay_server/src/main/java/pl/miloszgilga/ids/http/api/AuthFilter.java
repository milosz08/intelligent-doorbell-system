package pl.miloszgilga.ids.http.api;

import java.time.Instant;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.Constants;
import pl.miloszgilga.ids.http.html.AuthViewFilter;

@Authenticated
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuthViewFilter.class);

    private final SessionDao sessionDao;
    private final int sessionTtlSec;

    public AuthFilter(SessionDao sessionDao, int sessionTtlSec) {
        this.sessionDao = sessionDao;
        this.sessionTtlSec = sessionTtlSec;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            final String sessionId = requestContext.getHeaderString(Constants.SID_HEADER_NAME);
            if (sessionId == null || sessionId.isBlank()) {
                throw new LoginException("auth header not found");
            }
            final UserDetails userDetails = sessionDao.getSession(sessionId);
            if (userDetails == null) {
                throw new LoginException("session not exists");
            }
            final Instant updatedSessionTime = Instant.now().plusSeconds(sessionTtlSec);
            final boolean success = sessionDao.updateSessionTime(sessionId, updatedSessionTime);
            if (!success) {
                throw new LoginException("updating session time failed");
            }
            requestContext.setProperty("sessionId", sessionId);
            requestContext.setProperty("authenticatedUser", userDetails);
        } catch (LoginException ex) {
            LOG.error("Unable to perform authentication, cause: {}", ex.getMessage());
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("")
                    .type(MediaType.TEXT_PLAIN)
                    .build());
        }
    }
}
