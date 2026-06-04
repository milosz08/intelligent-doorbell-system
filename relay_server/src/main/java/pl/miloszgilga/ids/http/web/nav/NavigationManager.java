package pl.miloszgilga.ids.http.web.nav;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

public class NavigationManager {
    private static final Logger LOG = LoggerFactory.getLogger(NavigationManager.class);

    private final PermissionManager<Permission> permissionManager;

    public NavigationManager(PermissionManager<Permission> permissionManager) {
        this.permissionManager = permissionManager;
    }

    public List<NavigationLink> buildNavigation(UserDetails user, NavRootPage activePage) {
        final List<NavigationLink> accessibleLinks = new ArrayList<>();
        LOG.debug("Building navigation for user '{}', current page: '{}'", user.username(), activePage);
        for (final NavRootPage rootPage : NavRootPage.values()) {
            final boolean hasAccess = rootPage.getPermissions().length == 0 ||
                    permissionManager.hasAnyPermission(user.permissionsMask(), rootPage.getPermissions());
            if (hasAccess) {
                accessibleLinks.add(new NavigationLink(
                        rootPage.getTitle(),
                        "/" + rootPage.getPath(),
                        rootPage == activePage));
            } else {
                LOG.debug("Navigation link '{}' hidden for user '{}' (insufficient permissions)",
                        rootPage.getTitle(), user.username());
            }
        }
        return accessibleLinks;
    }
}
