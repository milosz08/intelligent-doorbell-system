package pl.miloszgilga.ids.http.html;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.http.Constants;

@PublicViewOnly
public class GuestViewFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        final Map<String, Cookie> cookies = requestContext.getCookies();
        if (cookies.containsKey(Constants.SID_COOKIE_NAME)) {
            requestContext.abortWith(
                    Response.seeOther(URI.create("/")).build());
        }
    }
}
