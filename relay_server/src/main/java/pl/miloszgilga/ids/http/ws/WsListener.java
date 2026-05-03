package pl.miloszgilga.ids.http.ws;

import java.util.UUID;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.dto.UserDetails;

public class WsListener implements Session.Listener.AutoDemanding {
    private static final Logger LOG = LoggerFactory.getLogger(WsListener.class);

    private final UserDetails userDetails;
    private final WsRouter wsRouter;
    private final WsSessionRegistry wsSessionRegistry;

    private String currentSessionId;

    public WsListener(UserDetails userDetails, WsRouter wsRouter, WsSessionRegistry wsSessionRegistry) {
        this.userDetails = userDetails;
        this.wsRouter = wsRouter;
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        currentSessionId = UUID.randomUUID().toString();
        wsSessionRegistry.register(currentSessionId, session, userDetails);
        LOG.info("New client connected, session ID: {}, role: {}, active clients: {}", currentSessionId,
                userDetails.role(), wsSessionRegistry.getSize());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        LOG.info("Client disconnected: {}, reason: {}, active clients: {}", getSafeSessionId(), reason,
                wsSessionRegistry.getSize());
        if (currentSessionId != null) {
            wsSessionRegistry.unregister(currentSessionId);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.error("Error occurred for session {}: {}", getSafeSessionId(), cause.getMessage(), cause);
        if (currentSessionId != null) {
            wsSessionRegistry.unregister(currentSessionId);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        LOG.debug("Received raw message from {}: {}", currentSessionId, message);
        wsRouter.route(message, currentSessionId);
    }

    private String getSafeSessionId() {
        return currentSessionId != null ? currentSessionId : "UNKNOWN_PENDING_SESSION";
    }
}
