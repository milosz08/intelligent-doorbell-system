package pl.miloszgilga.ids.http;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

public class AppSecurityContext implements SecurityContext {
    private static final Logger LOG = LoggerFactory.getLogger(AppSecurityContext.class);
    private static final String AUTH_SCHEME = "UNIVERSAL_AUTH";

    private final UserDetails userDetails;
    private final ContainerRequestContext requestContext;
    private final PermissionManager<Permission> permissionManager;

    public AppSecurityContext(UserDetails userDetails, ContainerRequestContext requestContext,
            PermissionManager<Permission> permissionManager) {
        this.userDetails = userDetails;
        this.requestContext = requestContext;
        this.permissionManager = permissionManager;
    }

    @Override
    public Principal getUserPrincipal() {
        return userDetails::username;
    }

    @Override
    public boolean isUserInRole(String roleOrPermission) {
        final boolean hasAccess = permissionManager.hasPermission(userDetails.permissionsMask(), roleOrPermission);
        if (hasAccess) {
            LOG.debug("Access GRANTED, user '{}' has required permission: '{}'", userDetails.username(),
                    roleOrPermission);
        } else {
            LOG.warn("Access DENIED, user '{}' missing permission: '{}' (current mask: {})",
                    userDetails.username(), roleOrPermission, userDetails.permissionsMask());
        }
        return hasAccess;
    }

    @Override
    public boolean isSecure() {
        return requestContext.getSecurityContext().isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return AUTH_SCHEME;
    }
}
