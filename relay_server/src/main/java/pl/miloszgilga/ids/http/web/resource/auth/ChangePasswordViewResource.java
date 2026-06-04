package pl.miloszgilga.ids.http.web.resource.auth;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.HttpWebPipelineException;
import pl.miloszgilga.ids.http.web.alert.FlashAlertManager;
import pl.miloszgilga.ids.http.web.auth.WebAuthenticated;
import pl.miloszgilga.ids.http.web.nav.NavigationManager;
import pl.miloszgilga.ids.http.web.resource.WebViewResourceBase;

@WebAuthenticated
@RequireDefaultPassword
@Path("/change-password")
public class ChangePasswordViewResource extends WebViewResourceBase {
    private final UserDao userDao;
    private final PasswordManager passwordManager;

    public ChangePasswordViewResource(HtmlTemplateEngine htmlTemplateEngine, NavigationManager navigationManager,
            UserDao userDao, PasswordManager passwordManager) {
        super(htmlTemplateEngine, navigationManager);
        this.userDao = userDao;
        this.passwordManager = passwordManager;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getChangePassword(@Context HttpServletRequest req) {
        final Map<String, Object> data = new HashMap<>();
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(AppHtmlView.CHANGE_PASSWORD, data))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response postChangePassword(@FormParam("currentPassword") String currentPassword,
            @FormParam("newPassword") String newPassword, @FormParam("repeatNewPassword") String repeatNewPassword,
            @Context HttpServletRequest req, @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, String> formErrors = new HashMap<>();
        try {
            if (Utils.isNullOrBlank(currentPassword)) {
                formErrors.put("currentPassword", "Current password must be set");
            }
            if (Utils.isNullOrBlank(newPassword)) {
                formErrors.put("newPassword", "New password must be set");
            }
            if (!newPassword.matches(Utils.REGEX_PASSWORD)) {
                formErrors.put("newPassword", "Password must have at least 8 charaters (max 40), " +
                        "one big and one small letter and one number");
            }
            if (!Objects.equals(newPassword, repeatNewPassword)) {
                throw new HttpWebPipelineException("Passwords are not the same");
            }
            if (formErrors.isEmpty()) {
                final UserDetails user = getSafetyLoggedUser(crc);
                if (!passwordManager.verify(user.username(), currentPassword)) {
                    throw new HttpWebPipelineException("Incorrect current password");
                }
                final String hashhedNewPassword = passwordManager.hash(newPassword);
                if (!userDao.updateUserPassword(user.username(), hashhedNewPassword, false)) {
                    throw new HttpWebPipelineException("Unable to change password");
                }
                return Response.seeOther(URI.create("/"))
                        .cookie(FlashAlertManager.setSuccess(req, "Successfully updated password"))
                        .build();
            }
        } catch (HttpWebPipelineException ex) {
            FlashAlertManager.setDanger(req, ex.getMessage());
        } finally {
            data.put("formErrors", formErrors);
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(AppHtmlView.CHANGE_PASSWORD, data))
                .cookie(killCookie)
                .build();
    }

    @POST
    @Path("/keep")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response postKeepPassword(@FormParam("doNotCheck") boolean doNotCheck, @Context HttpServletRequest req,
            @Context ContainerRequestContext crc) {
        final Map<String, Object> data = new HashMap<>();
        try {
            final UserDetails user = getSafetyLoggedUser(crc);
            if (!userDao.setDoNotCheckPassword(user.id(), doNotCheck)) {
                throw new HttpWebPipelineException("Unable to set do not change password");
            }
            String alertMessage = "Password was not updated";
            if (doNotCheck) {
                alertMessage += "and update password form was disabled";
            }
            return Response.seeOther(URI.create("/"))
                    .cookie(FlashAlertManager.setWarning(req, alertMessage))
                    .build();
        } catch (HttpWebPipelineException ex) {
            data.put("doNotCheck", doNotCheck);
            FlashAlertManager.setDanger(req, ex.getMessage());
        }
        final NewCookie killCookie = FlashAlertManager.consume(req, data);
        return Response.ok(parseTemplate(AppHtmlView.CHANGE_PASSWORD, data))
                .cookie(killCookie)
                .build();
    }
}
