package pl.miloszgilga.ids.http.web.auth;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.http.Constants;

@PublicViewOnly
public class GuestViewFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(GuestViewFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final Map<String, Cookie> cookies = requestContext.getCookies();
        LOG.debug("Checking request to {} for existing session cookie", requestContext.getUriInfo().getPath());
        if (cookies.containsKey(Constants.SID_COOKIE_NAME)) {
            LOG.debug("Found active session cookie, redirecting guest from public view to dashboard (/)");
            requestContext.abortWith(
                    Response.seeOther(URI.create("/")).build());
        }
    }
}
