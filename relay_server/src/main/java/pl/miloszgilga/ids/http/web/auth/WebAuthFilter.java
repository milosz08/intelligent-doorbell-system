package pl.miloszgilga.ids.http.web.auth;

import java.net.URI;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.http.BaseAuthFilter;
import pl.miloszgilga.ids.http.Constants;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

@WebAuthenticated
public class WebAuthFilter extends BaseAuthFilter {
    public WebAuthFilter(SessionDao sessionDao, PermissionManager<Permission> permissionManager, int sessionTtlSec) {
        super(sessionDao, permissionManager, sessionTtlSec);
    }

    @Override
    protected void reject(ContainerRequestContext ctx) {
        final NewCookie killCookie = new NewCookie.Builder(Constants.SID_COOKIE_NAME)
                .value("")
                .path("/")
                .maxAge(0)
                .build();
        ctx.abortWith(Response.seeOther(URI.create("/login"))
                .cookie(killCookie)
                .build());
    }
}
