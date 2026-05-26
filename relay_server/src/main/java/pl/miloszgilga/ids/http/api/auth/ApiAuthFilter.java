package pl.miloszgilga.ids.http.api.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.http.BaseAuthFilter;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

@ApiAuthenticated
public class ApiAuthFilter extends BaseAuthFilter {
    public ApiAuthFilter(SessionDao sessionDao, PermissionManager<Permission> permissionManager, int sessionTtlSec) {
        super(sessionDao, permissionManager, sessionTtlSec);
    }

    @Override
    protected void reject(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("")
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}
