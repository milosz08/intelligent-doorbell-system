package pl.miloszgilga.ids.http.html;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.Role;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.Constants;

@ViewAuthenticated
public class AuthViewFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuthViewFilter.class);

    private final SessionDao sessionDao;
    private final int sessionTtlSec;

    public AuthViewFilter(SessionDao sessionDao, int sessionTtlSec) {
        this.sessionDao = sessionDao;
        this.sessionTtlSec = sessionTtlSec;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Map<String, Cookie> cookies = requestContext.getCookies();
        try {
            if (!cookies.containsKey(Constants.SID_COOKIE_NAME)) {
                throw new LoginException("session cookie not exists");
            }
            final Cookie sessionCookie = cookies.get(Constants.SID_COOKIE_NAME);
            final String sessionId = sessionCookie.getValue();
            final UserDetails userDetails = sessionDao.getSession(sessionId);
            if (userDetails == null) {
                throw new LoginException("session not exists");
            }
            if (!userDetails.role().equals(Role.ADMIN)) {
                throw new LoginException("attempt to login without admin account");
            }
            final Instant updatedSessionTime = Instant.now().plusSeconds(sessionTtlSec);
            final boolean success = sessionDao.updateSessionTime(sessionId, updatedSessionTime);
            if (!success) {
                throw new LoginException("updating session time failed");
            }
            final NewCookie refreshedCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                    .value(sessionCookie.getValue())
                    .path("/")
                    .maxAge(sessionTtlSec)
                    .httpOnly(true)
                    .build();
            requestContext.setProperty("refreshCookie", refreshedCookie);
            requestContext.setProperty("sessionId", sessionId);
            requestContext.setProperty("authenticatedUser", userDetails);
        } catch (LoginException ex) {
            LOG.error("Unable to perform authentication, cause: {}", ex.getMessage());
            requestContext.abortWith(Response.seeOther(URI.create("/login")).build());
        }
    }
}
