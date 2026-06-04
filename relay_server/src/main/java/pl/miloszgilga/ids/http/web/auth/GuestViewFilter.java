package pl.miloszgilga.ids.http.web.auth;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.http.Constants;

@PublicViewOnly
public class GuestViewFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(GuestViewFilter.class);

    private final SessionDao sessionDao;

    public GuestViewFilter(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final Map<String, Cookie> cookies = requestContext.getCookies();
        LOG.debug("Checking request to {} for existing session cookie", requestContext.getUriInfo().getPath());
        if (!cookies.containsKey(Constants.SID_COOKIE_NAME)) {
            return;
        }
        final String sessionId = cookies.get(Constants.SID_COOKIE_NAME).getValue();
        if (sessionDao.getSession(sessionId) != null) {
            LOG.debug("Found VALID session cookie, redirecting guest to dashboard (/)");
            requestContext.abortWith(Response.seeOther(URI.create("/")).build());
            return;
        }
        LOG.debug("Found INVALID/EXPIRED session cookie, clearing it and showing login page");
        final NewCookie killCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                .value("")
                .path("/")
                .maxAge(0)
                .build();
        requestContext.setProperty("refreshCookie", killCookie);
    }
}
