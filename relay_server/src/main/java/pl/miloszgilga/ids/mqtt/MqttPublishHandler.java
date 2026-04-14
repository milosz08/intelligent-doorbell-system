package pl.miloszgilga.ids.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.util.ReferenceCountUtil;

class MqttPublishHandler extends AbstractInterceptHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MqttPublishHandler.class);

    private final ExecutorService executorService;
    private final Map<String, MqttMessageHandler> routes = new HashMap<>();
    private final MqttConnectionCallback connectionCallback;

    MqttPublishHandler(List<MqttMessageHandler> handlers, ExecutorService executorService,
            MqttConnectionCallback connectionCallback) {
        for (final MqttMessageHandler handler : handlers) {
            final String path = handler.getTopic().getPath();
            routes.put(path, handler);
            LOG.info("Registered handler for topic: {}", path);
        }
        this.executorService = executorService;
        this.connectionCallback = connectionCallback;
    }

    @Override
    public String getID() {
        return "vt-mqtt-publisher-handler";
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        final String topic = msg.getTopicName();
        final String payload = extractPayloadAndReleaseMemory(msg);
        executorService.submit(() -> processMessage(topic, payload));
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        final String clientId = msg.getClientID();
        final String username = msg.getUsername();
        onConnectionCallback(() -> connectionCallback.onConnected(clientId, username));
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        final String clientId = msg.getClientID();
        onConnectionCallback(() -> connectionCallback.onDisconnected(clientId, false));
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        final String clientId = msg.getClientID();
        onConnectionCallback(() -> connectionCallback.onDisconnected(clientId, true));
    }

    @Override
    public void onSessionLoopError(Throwable throwable) {
        LOG.error("Session loop error in {}", getID(), throwable);
    }

    private void onConnectionCallback(Runnable runnable) {
        executorService.submit(() -> {
            if (connectionCallback != null) {
                runnable.run();
            }
        });
    }

    private String extractPayloadAndReleaseMemory(InterceptPublishMessage msg) {
        if (msg.getPayload() == null) {
            return "";
        }
        try {
            return msg.getPayload().toString(StandardCharsets.UTF_8);
        } finally {
            // release netty's native memory
            ReferenceCountUtil.release(msg.getPayload());
        }
    }

    private void processMessage(String topic, String payload) {
        final MqttMessageHandler handler = routes.get(topic);
        if (handler == null) {
            LOG.trace("Message rejected - no registered handler for topic: {}", topic);
            return;
        }
        if (isPayloadEmpty(payload)) {
            LOG.debug("Received empty payload for topic: {}, executing handler with empty JSON.", topic);
            safeInvokeHandler(handler, new JsonObject());
            return;
        }
        parseAndHandleJson(handler, topic, payload);
    }

    private boolean isPayloadEmpty(String payload) {
        return payload == null || payload.trim().isEmpty();
    }

    private void parseAndHandleJson(MqttMessageHandler handler, String topic, String payload) {
        try {
            final JsonElement jsonElement = JsonParser.parseString(payload);
            if (jsonElement.isJsonObject()) {
                safeInvokeHandler(handler, jsonElement.getAsJsonObject());
            } else {
                LOG.warn("Payload for topic {} is not a JSON Object, received: {}", topic, payload);
            }
        } catch (JsonSyntaxException ex) {
            LOG.warn("Invalid JSON syntax for topic {}, payload: '{}'", topic, payload);
        }
    }

    private void safeInvokeHandler(MqttMessageHandler handler, JsonObject json) {
        try {
            handler.handle(json);
        } catch (Exception ex) {
            LOG.error("Error occurred while executing handler for topic: {}, error: {}",
                    handler.getTopic().getPath(), ex.getMessage(), ex);
        }
    }
}
