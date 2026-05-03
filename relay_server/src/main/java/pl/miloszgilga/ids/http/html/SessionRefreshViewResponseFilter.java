package pl.miloszgilga.ids.http.html;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;

public class SessionRefreshViewResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final Object cookie = requestContext.getProperty("refresh-cookie");
        if (cookie instanceof NewCookie newCookie) {
            responseContext.getCookies().put("JSESSIONID", newCookie);
        }
    }
}
