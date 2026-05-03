package pl.miloszgilga.ids.http.ws;

import java.util.List;

import javax.security.auth.login.LoginException;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.Constants;

public class JettyWsCreator implements WebSocketCreator {
    private static final Logger LOG = LoggerFactory.getLogger(JettyWsCreator.class);

    private final SessionDao sessionDao;
    private final WsRouter wsRouter;
    private final WsSessionRegistry wsSessionRegistry;

    public JettyWsCreator(SessionDao sessionDao, WsRouter wsRouter, WsSessionRegistry wsSessionRegistry) {
        this.sessionDao = sessionDao;
        this.wsRouter = wsRouter;
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public Object createWebSocket(ServerUpgradeRequest request, ServerUpgradeResponse response, Callback callback) {
        LOG.debug("Starting WebSocket upgrade request for URI: {}", request.getHttpURI().asString());
        String sessionId = null;
        final HttpFields headers = request.getHeaders();
        if (headers.contains(Constants.SID_HEADER_NAME)) {
            sessionId = headers.get(Constants.SID_HEADER_NAME);
            LOG.debug("Found session ID in headers: {}", sessionId);
        } else {
            final List<HttpCookie> cookies = Request.getCookies(request);
            LOG.debug("Header SID not found, checking {} cookies", cookies.size());
            for (final HttpCookie cookie : cookies) {
                if (cookie.getName().equals(Constants.SID_COOKIE_NAME)) {
                    sessionId = cookie.getValue();
                    LOG.debug("Found session ID in cookie: {}", sessionId);
                    break;
                }
            }
        }
        try {
            if (sessionId == null) {
                throw new LoginException("session token not provided");
            }
            final UserDetails userDetails = sessionDao.getSession(sessionId);
            if (userDetails == null) {
                throw new LoginException("session not exists");
            }
            LOG.debug("Authentication successful for user: {} (role: {})", userDetails.username(),
                    userDetails.role());
            return new WsListener(userDetails, wsRouter, wsSessionRegistry);
        } catch (LoginException ex) {
            LOG.error("Unable to authenticate to WS, cause: {}", ex.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            callback.succeeded();
            return null;
        }
    }
}
