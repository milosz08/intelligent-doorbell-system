package pl.miloszgilga.ids.http.ws;

import java.util.UUID;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

public class WsListener implements Session.Listener.AutoDemanding {
    private static final Logger LOG = LoggerFactory.getLogger(WsListener.class);

    private final UserDetails userDetails;
    private final WsRouter wsRouter;
    private final WsSessionRegistry wsSessionRegistry;
    private final PermissionManager<Permission> permissionManager;

    private String currentSessionId;

    public WsListener(UserDetails userDetails, WsRouter wsRouter, WsSessionRegistry wsSessionRegistry,
            PermissionManager<Permission> permissionManager) {
        this.userDetails = userDetails;
        this.wsRouter = wsRouter;
        this.wsSessionRegistry = wsSessionRegistry;
        this.permissionManager = permissionManager;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        currentSessionId = UUID.randomUUID().toString();
        wsSessionRegistry.register(currentSessionId, session, userDetails);
        LOG.info("New client connected, session ID: {}, permissions: {}, active clients: {}", currentSessionId,
                permissionManager.getActivePermissionsAsStrings(userDetails.permissionsMask()),
                wsSessionRegistry.getSize());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        LOG.info("Client disconnected: {}, reason: {}, active clients: {}", getSafeSessionId(), reason,
                wsSessionRegistry.getSize() - 1);
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
