package pl.miloszgilga.ids.http.ws;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pl.miloszgilga.ids.db.Role;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.http.ws.op.OpCode;

public class WsSessionRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(WsSessionRegistry.class);

    private final Map<String, WsSessionData> sessions = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void register(String sessionId, Session session, UserDetails userDetails) {
        sessions.put(sessionId, new WsSessionData(sessionId, session, userDetails));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void sendTo(String sessionId, OpCode op, Object data) {
        if (!sessions.containsKey(sessionId)) {
            LOG.warn("Cannot send message: session {} is unknown or closed", sessionId);
            return;
        }
        final WsSessionData sessionData = sessions.get(sessionId);
        final WsMessage message = createWsMessage(op, data);
        final String payload = gson.toJson(message);
        try {
            LOG.debug("Sending op: {} to session: {}", message.op(), sessionId);
            await(callback -> sessionData.session().sendText(payload, callback));
        } catch (Exception ex) {
            LOG.error("Direct send failed for session {}: {}", sessionId, ex.getMessage());
        }
    }

    public void sendTo(String sessionId, OpCode op) {
        sendTo(sessionId, op, null);
    }

    public void broadcast(OpCode op, Object data, Set<Role> roles) {
        if (sessions.isEmpty()) {
            LOG.debug("Broadcast skipped: no active sessions for op: {}", op.toString());
            return;
        }
        final WsMessage message = createWsMessage(op, data);
        final String payload = gson.toJson(message);
        LOG.debug("Broadcasting op: {} to {} active clients", message.op(), getSize());

        for (final WsSessionData sessionData : sessions.values()) {
            final Session session = sessionData.session();
            if (session.isOpen() && (roles == null || roles.contains(sessionData.user().role()))) {
                virtualExecutor.submit(() -> {
                    try {
                        await(callback -> session.sendText(payload, callback));
                    } catch (Exception ex) {
                        LOG.error("Broadcast failed for session {}: {}", sessionData.sessionId(),
                                ex.getMessage());
                    }
                });
            }
        }
    }

    public void broadcast(OpCode op, Object data) {
        broadcast(op, data, null);
    }

    public void broadcast(OpCode op) {
        broadcast(op, null, null);
    }

    public void broadcast(OpCode op, Set<Role> roles) {
        broadcast(op, null, roles);
    }

    public int getSize() {
        return sessions.size();
    }

    private WsMessage createWsMessage(OpCode op, Object data) {
        return new WsMessage(op.getCode(), gson.toJsonTree(data));
    }

    private void await(Consumer<Callback> action) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final Callback callback = new Callback() {
            @Override
            public void fail(Throwable cause) {
                future.completeExceptionally(cause);
            }

            @Override
            public void succeed() {
                future.complete(null);
            }
        };
        try {
            action.accept(callback);
            future.join();
        } catch (CompletionException ex) {
            throw ex;
        }
    }
}
