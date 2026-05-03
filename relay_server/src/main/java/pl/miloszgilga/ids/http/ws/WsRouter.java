package pl.miloszgilga.ids.http.ws;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pl.miloszgilga.ids.http.ws.handler.WsMessageHandler;
import pl.miloszgilga.ids.http.ws.op.OpCode;

public class WsRouter {
    private static final Logger LOG = LoggerFactory.getLogger(WsRouter.class);

    private final Map<OpCode, WsMessageHandler> handlers = new HashMap<>();
    private final OpCode fallback = () -> OpCode.combine(0, 0);

    public void registerHandler(WsMessageHandler handler) {
        handlers.put(handler.getOpCode(), handler);
        LOG.info("Registered handler for OpCode: {}", handler.getOpCode());
    }

    public void route(String rawMessage, String sessionId) {
        try {
            final JsonObject obj = JsonParser.parseString(rawMessage).getAsJsonObject();
            if (!obj.has("op")) {
                LOG.warn("Missing 'op' field from session: {}", sessionId);
                return;
            }
            final int rawOp = obj.get("op").getAsInt();
            final OpCode opCode = OpCode.fromInt(rawOp, fallback);

            LOG.debug("Raw: {}, category: {}, action: {}", rawOp, opCode.getCategory(),
                    opCode.getActionCode());

            final JsonElement data = obj.get("data");
            if (data != null && !data.isJsonNull() && !data.isJsonObject()) {
                LOG.warn("Invalid data format from {}, expected JsonObject or null",
                        sessionId);
                return;
            }
            final WsMessageHandler handler = handlers.get(opCode);
            if (handler != null) {
                final JsonObject payload = (data != null && data.isJsonObject())
                        ? data.getAsJsonObject()
                        : new JsonObject();
                handler.handle(sessionId, payload);
            } else {
                LOG.warn("No handler for category: {}, action: {}", opCode.getCategory(),
                        opCode.getActionCode());
            }
        } catch (Exception ex) {
            LOG.error("Processing error: {}", ex.getMessage());
        }
    }
}
