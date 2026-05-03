package pl.miloszgilga.ids.mqtt.handler;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.mqtt.MqttMessageHandler;
import pl.miloszgilga.ids.mqtt.topic.MqttAppInboundTopic;
import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;

public class MqttOnDoorbellClientRingHandler implements MqttMessageHandler {
    private final WsSessionRegistry wsSessionRegistry;

    public MqttOnDoorbellClientRingHandler(WsSessionRegistry wsSessionRegistry) {
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public MqttInboundTopic getTopic() {
        return MqttAppInboundTopic.ON_DOORBELL_RING;
    }

    @Override
    public void handle(JsonObject jsonPayload) {
        wsSessionRegistry.broadcast(AppOpCode.DOORBELL_MANUALLY_RING);
    }
}
