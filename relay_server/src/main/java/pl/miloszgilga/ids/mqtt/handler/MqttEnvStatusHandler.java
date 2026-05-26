package pl.miloszgilga.ids.mqtt.handler;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.mqtt.MqttMessageHandler;
import pl.miloszgilga.ids.mqtt.topic.MqttAppInboundTopic;
import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;
import pl.miloszgilga.ids.security.Permission;

public class MqttEnvStatusHandler implements MqttMessageHandler {
    private final WsSessionRegistry wsSessionRegistry;

    public MqttEnvStatusHandler(WsSessionRegistry wsSessionRegistry) {
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public MqttInboundTopic getTopic() {
        return MqttAppInboundTopic.ENV_STATUS;
    }

    @Override
    public void handle(JsonObject jsonPayload) {
        // TODO: persist in db
        wsSessionRegistry.broadcast(AppOpCode.ENV_STATUS, jsonPayload, Permission.ENV_STATUS_VIEWER);
    }
}
