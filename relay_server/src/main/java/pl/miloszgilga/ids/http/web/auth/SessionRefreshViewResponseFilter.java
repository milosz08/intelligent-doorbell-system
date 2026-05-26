package pl.miloszgilga.ids.http.web.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;
import pl.miloszgilga.ids.http.Constants;

public class SessionRefreshViewResponseFilter implements ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(SessionRefreshViewResponseFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final Object cookie = requestContext.getProperty("refreshCookie");
        if (cookie instanceof NewCookie newCookie) {
            responseContext.getHeaders().add(Constants.SID_COOKIE_NAME, newCookie);
            LOG.debug("Attached refreshed session cookie ({}) to response", Constants.SID_COOKIE_NAME);
        } else {
            LOG.debug("No refresh cookie found in context");
        }
    }
}
