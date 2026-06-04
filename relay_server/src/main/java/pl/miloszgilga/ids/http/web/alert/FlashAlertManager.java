package pl.miloszgilga.ids.http.web.alert;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.NewCookie;

public class FlashAlertManager {
    private static final Logger LOG = LoggerFactory.getLogger(FlashAlertManager.class);

    private static final Map<String, FlashAlert> ALERT_STORAGE = new ConcurrentHashMap<>();
    private static final String ALERT_REQ_KEY = "flash_alert_attr";
    private static final String COOKIE_NAME = "JALERTID";

    public static NewCookie setInfo(HttpServletRequest req, String message, Object... args) {
        return setAlert(req, FlashAlert.info(String.format(message, args)));
    }

    public static NewCookie setSuccess(HttpServletRequest req, String message,
            Object... args) {
        return setAlert(req, FlashAlert.success(String.format(message, args)));
    }

    public static NewCookie setWarning(HttpServletRequest req, String message,
            Object... args) {
        return setAlert(req, FlashAlert.warning(String.format(message, args)));
    }

    public static NewCookie setDanger(HttpServletRequest req, String message, Object... args) {
        return setAlert(req, FlashAlert.danger(String.format(message, args)));
    }

    private static NewCookie setAlert(HttpServletRequest req, FlashAlert alert) {
        if (req != null) {
            req.setAttribute(ALERT_REQ_KEY, alert);
        }
        final String uuid = UUID.randomUUID().toString();
        ALERT_STORAGE.put(uuid, alert);

        final NewCookie cookie = new NewCookie.Builder(COOKIE_NAME)
                .value(uuid)
                .path("/")
                .httpOnly(true)
                .build();
        LOG.debug("Set new {} alert at: {}", alert.type(), req == null ? "DIRECTLY" : req.getRequestURI());
        return cookie;
    }

    public static NewCookie consume(HttpServletRequest req, Map<String, Object> model) {
        NewCookie killCookie = null;
        FlashAlert alert = (FlashAlert) req.getAttribute(ALERT_REQ_KEY);
        final Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    alert = ALERT_STORAGE.remove(cookie.getValue());
                    if (alert != null) {
                        killCookie = new NewCookie.Builder(COOKIE_NAME)
                                .value("")
                                .path("/")
                                .maxAge(0)
                                .httpOnly(true)
                                .build();
                    }
                    break;
                }
            }
        }
        if (alert != null) {
            model.put("alert", alert);
            LOG.debug("Extracted {} alert and inserted into view model", alert.type());
        }
        return killCookie;
    }
}
