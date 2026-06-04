package pl.miloszgilga.ids.http;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

public abstract class BaseAuthFilter implements ContainerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final SessionDao sessionDao;
    private final PermissionManager<Permission> permissionManager;
    private final int sessionTtlSec;

    protected BaseAuthFilter(SessionDao sessionDao, PermissionManager<Permission> permissionManager,
            int sessionTtlSec) {
        this.sessionDao = sessionDao;
        this.permissionManager = permissionManager;
        this.sessionTtlSec = sessionTtlSec;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String sessionId = requestContext.getHeaderString(Constants.SID_HEADER_NAME);
            if (sessionId == null) {
                final Map<String, Cookie> cookies = requestContext.getCookies();
                if (cookies.containsKey(Constants.SID_COOKIE_NAME)) {
                    sessionId = cookies.get(Constants.SID_COOKIE_NAME).getValue();
                }
            }
            if (sessionId == null) {
                throw new LoginException("No session token found in headers or cookies");
            }
            final UserDetails userDetails = sessionDao.getSession(sessionId);
            if (userDetails == null) {
                throw new LoginException("Session does not exist or expired");
            }
            if (!userDetails.isActive()) {
                throw new LoginException("User account is inactive");
            }
            final Instant updatedSessionTime = Instant.now().plusSeconds(sessionTtlSec);
            if (!sessionDao.updateSessionTime(sessionId, updatedSessionTime)) {
                throw new LoginException("Updating session time failed");
            }
            final NewCookie refreshedCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                    .value(sessionId)
                    .path("/")
                    .maxAge(sessionTtlSec)
                    .httpOnly(true)
                    .build();
            requestContext.setProperty("refreshCookie", refreshedCookie);
            requestContext.setSecurityContext(new AppSecurityContext(userDetails, requestContext, permissionManager));
            requestContext.setProperty("sessionId", sessionId);
            requestContext.setProperty("authenticatedUser", userDetails);
        } catch (LoginException ex) {
            log.warn("Authentication failed: {}", ex.getMessage());
            reject(requestContext);
        }
    }

    protected abstract void reject(ContainerRequestContext ctx);
}
