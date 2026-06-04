package pl.miloszgilga.ids.http.web.resource.user;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.HttpWebPipelineException;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;
import pl.miloszgilga.ids.http.web.auth.WebAuthenticated;
import pl.miloszgilga.ids.http.web.nav.NavRootPage;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;
import pl.miloszgilga.ids.http.web.resource.WebViewResourceBase;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

@WebAuthenticated
@Path("/users")
public class UsersViewResource extends WebViewResourceBase {
    private final UserDao userDao;
    private final PermissionManager<Permission> permissionManager;
    private final PasswordManager passwordManager;
    private final int passwordLength;

    public UsersViewResource(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager,
            UserDao userDao, PermissionManager<Permission> permissionManager, PasswordManager passwordManager,
            int passwordLength) {
        super(htmlTemplateEngine, navigationManager);
        this.userDao = userDao;
        this.permissionManager = permissionManager;
        this.passwordManager = passwordManager;
        this.passwordLength = passwordLength;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getUsers(@Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        final List<UserDetails> users = userDao.getUsers();

        final List<PermissionsModel> permissions = new ArrayList<>();
        for (final Permission permission : permissionManager.getPermissions()) {
            final int countOfUsers = permissionManager.countEntitiesWithPermission(users, permission);
            permissions.add(new PermissionsModel(
                    permission.name(),
                    permissionManager.formatBitmaskString(permission),
                    permission.getBit(),
                    permission.isAdmin(),
                    countOfUsers));
        }
        Collections.sort(permissions);
        data.put("users", users);
        data.put("permissions", permissions);

        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.USERS, data, NavRootPage.USERS_AND_PRIVILEGES))
                .cookie(killCookie)
                .build();
    }

    @GET
    @Path("/add-user")
    @Produces(MediaType.TEXT_HTML)
    public Response getAddUser(@Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        data.put("permissions", parsePermissions());
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.ADD_USER, data, NavRootPage.USERS_AND_PRIVILEGES))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Path("/add-user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response addUser(MultivaluedMap<String, String> formParams, @Context HttpServletRequest req,
            @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, String> formErrors = new HashMap<>();

        final String username = formParams.getFirst("username");
        final boolean isActive = formParams.containsKey("isActive");
        String password = formParams.getFirst("password");
        String successInfo = "Successfully added new user";
        final List<String> checkedPrivileges = formParams.containsKey("privileges")
                ? formParams.get("privileges")
                : List.of();
        try {
            if (Utils.isNullOrBlank(username)) {
                formErrors.put("username", "Username/login must be set");
            } else if (!username.matches(Utils.REGEX_USERNAME)) {
                formErrors.put("username", "Username/login must have at least 3 characters (max 20), " +
                        "and contain only small letters with numbers");
            }
            if (Utils.isNullOrBlank(password)) {
                password = Utils.generateSecurePassword(passwordLength);
                successInfo += " with password " + password;
            } else if (!password.matches(Utils.REGEX_PASSWORD)) {
                formErrors.put("password", "Password must have at least 8 characters (max 40), " +
                        "one big and one small letter and one number");
            }
            if (userDao.userExists(username)) {
                throw new HttpWebPipelineException("User with this username already exists");
            }
            if (formErrors.isEmpty()) {
                final long permissionMask = permissionManager.generateMask(checkedPrivileges);
                final String hashedPassword = passwordManager.hash(password);
                if (!userDao.createUser(username, hashedPassword, isActive, permissionMask, false)) {
                    throw new HttpWebPipelineException("Unable to create user");
                }
                return Response.seeOther(URI.create("/users"))
                        .cookie(FlashAlertManager.setSuccess(req, successInfo))
                        .build();
            }
        } catch (HttpWebPipelineException ex) {
            FlashAlertManager.setDanger(req, ex.getMessage());
        } finally {
            data.put("username", username);
            data.put("formErrors", formErrors);
            data.put("isActive", isActive);
            data.put("permissions", parsePermissions(permission -> checkedPrivileges.contains(permission.name())));
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.ADD_USER, data, NavRootPage.USERS_AND_PRIVILEGES))
                .cookie(killCookie)
                .build();
    }

    @GET
    @Path("/privileges/{userId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getUserPrivileges(@PathParam("userId") long userId, @Context HttpServletRequest req,
            @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        try {
            final UserDetails user = getUserDetails(userId);
            data.put("user", user);
            data.put("permissions", parsePermissions(permission -> permissionManager
                    .hasPermission(user.permissionsMask(), permission.name(), false)));
        } catch (HttpWebPipelineException ex) {
            FlashAlertManager.setDanger(req, ex.getMessage());
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.USER_PRIVILEGES, data, NavRootPage.USERS_AND_PRIVILEGES))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Path("/privileges/{userId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updateUserPrivileges(@PathParam("userId") long userId, MultivaluedMap<String, String> formParams,
            @Context HttpServletRequest req, @Context HttpServletResponse res, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        try {
            final UserDetails user = getUserDetails(userId);
            if (user.isSystemAccount()) {
                throw new HttpWebPipelineException("You cannot change permissions for system account");
            }
            final List<String> checkedPrivileges = formParams.get("privileges");
            if (!userDao.setPermissions(userId, permissionManager.generateMask(checkedPrivileges))) {
                throw new HttpWebPipelineException("Unable to update account permissions");
            }
            return Response.seeOther(URI.create("/users"))
                    .cookie(FlashAlertManager.setSuccess(req,
                            "Successfully updated user '%s' privileges", user.username()))
                    .build();
        } catch (HttpWebPipelineException ex) {
            FlashAlertManager.setDanger(req, ex.getMessage());
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(crc, AppHtmlView.USER_PRIVILEGES, data, NavRootPage.USERS_AND_PRIVILEGES))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Path("/account-state/{userId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updateAccountStatus(@PathParam("userId") long userId, @FormParam("isActive") boolean isActive,
            @Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final ResponseBuilder responseBuilder = Response.seeOther(URI.create("/users"));
        try {
            final UserDetails user = getUserDetails(userId);
            if (user.isSystemAccount()) {
                throw new HttpWebPipelineException("You cannot change system account active/inactive state");
            }
            if (!userDao.setAccountState(userId, isActive)) {
                throw new HttpWebPipelineException("Unable to update account state");
            }
            responseBuilder.cookie(FlashAlertManager.setSuccess(req,
                    "Successfully %s user '%s' account", isActive ? "activated" : "deactivated",
                    user.username()));
        } catch (HttpWebPipelineException ex) {
            responseBuilder.cookie(FlashAlertManager.setDanger(req, ex.getMessage()));
        }
        return responseBuilder.build();
    }

    @POST
    @Path("/delete/{userId}")
    @Produces(MediaType.TEXT_HTML)
    public Response deleteAccount(@PathParam("userId") long userId, @Context HttpServletRequest req,
            @Context ContainerRequestContext crc) {
        final ResponseBuilder responseBuilder = Response.seeOther(URI.create("/users"));
        try {
            final UserDetails user = getUserDetails(userId);
            if (user.isSystemAccount()) {
                throw new HttpWebPipelineException("You cannot delete system account");
            }
            if (!userDao.deleteUser(userId)) {
                throw new HttpWebPipelineException("Unable to delete user");
            }
            responseBuilder.cookie(FlashAlertManager.setSuccess(req,
                    "Successfully deleted user '%s'", user.username()));
        } catch (HttpWebPipelineException ex) {
            responseBuilder.cookie(FlashAlertManager.setDanger(req, ex.getMessage()));
        }
        return responseBuilder.build();
    }

    @POST
    @Path("/revoke-password/{userId}")
    @Produces(MediaType.TEXT_HTML)
    public Response revokePassword(@PathParam("userId") long userId, @Context HttpServletRequest req,
            @Context ContainerRequestContext crc) {
        final ResponseBuilder responseBuilder = Response.seeOther(URI.create("/users"));
        try {
            final UserDetails user = getUserDetails(userId);
            if (user.isSystemAccount()) {
                throw new HttpWebPipelineException("You cannot revoke password for system account");
            }
            final String password = Utils.generateSecurePassword(passwordLength);
            final String passwordHash = passwordManager.hash(password);
            if (!userDao.updateUserPassword(user.username(), passwordHash, true)) {
                throw new HttpWebPipelineException("Unable to update user password");
            }
            responseBuilder.cookie(FlashAlertManager.setSuccess(req,
                    "Successfully revoked password for user '%s', generated password: %s",
                    user.username(), password));
        } catch (HttpWebPipelineException ex) {
            responseBuilder.cookie(FlashAlertManager.setDanger(req, ex.getMessage()));
        }
        return responseBuilder.build();
    }

    private List<PermissionModel> parsePermissions(Function<Permission, Boolean> isActive) {
        final List<PermissionModel> permissions = new ArrayList<>();
        for (final Permission permission : Permission.values()) {
            permissions.add(new PermissionModel(
                    permission.name(),
                    permissionManager.formatBitmaskString(permission),
                    permission.getBit(),
                    permission.isAdmin(),
                    isActive.apply(permission)));
        }
        return permissions;
    }

    private List<PermissionModel> parsePermissions() {
        return parsePermissions(permission -> false);
    }

    private UserDetails getUserDetails(long userId) {
        final UserDetails user = userDao.getUserDetails(userId);
        if (user == null) {
            throw new HttpWebPipelineException("Unable to find user");
        }
        return user;
    }
}
