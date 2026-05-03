package pl.miloszgilga.ids.mqtt.handler;

import java.util.Map;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.mqtt.MqttMessageHandler;
import pl.miloszgilga.ids.mqtt.topic.MqttAppInboundTopic;
import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;

public class MqttOnDoorbellModeSetHandler implements MqttMessageHandler {
    private final WsSessionRegistry wsSessionRegistry;

    public MqttOnDoorbellModeSetHandler(WsSessionRegistry wsSessionRegistry) {
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public MqttInboundTopic getTopic() {
        return MqttAppInboundTopic.ON_DOORBELL_MODE_SET;
    }

    @Override
    public void handle(JsonObject jsonPayload) {
        final boolean isSilent = jsonPayload.get("is_silent").getAsBoolean();
        // TODO: persist in db
        wsSessionRegistry.broadcast(AppOpCode.DOORBELL_MODE_SET, Map.of(
                "isSilent", isSilent,
                "updatedAt", System.currentTimeMillis()));
    }
}
