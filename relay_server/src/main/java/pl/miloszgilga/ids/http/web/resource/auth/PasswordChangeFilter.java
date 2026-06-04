package pl.miloszgilga.ids.http.web.resource.auth;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;

@RequireDefaultPassword
@Priority(Priorities.AUTHORIZATION + 10)
public class PasswordChangeFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordChangeFilter.class);

    private final UserDao userDao;

    public PasswordChangeFilter(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void filter(ContainerRequestContext crc) throws IOException {
        final UserDetails user = (UserDetails) crc.getProperty("authenticatedUser");
        if (user == null) {
            LOG.debug("No authenticated user found in context");
            return;
        }
        final boolean hasDefaultPassword = userDao.userHasDefaultPassword(user.username());
        LOG.debug("User '{}', hasDefaultPassword: {}", user.username(), hasDefaultPassword);
        if (!hasDefaultPassword) {
            LOG.debug("User '{}' already changed password, redirecting to /", user.username());
            crc.abortWith(Response.seeOther(URI.create("/"))
                    .cookie(FlashAlertManager.setWarning(null, "Password already changed"))
                    .build());
        } else {
            LOG.debug("User '{}' still has default password, allowing access", user.username());
        }
    }
}
