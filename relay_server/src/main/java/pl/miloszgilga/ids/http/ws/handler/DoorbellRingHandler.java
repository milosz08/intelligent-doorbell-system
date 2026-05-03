package pl.miloszgilga.ids.http.ws.handler;

import com.google.gson.JsonObject;

import io.netty.handler.codec.mqtt.MqttQoS;
import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.http.ws.op.OpCode;
import pl.miloszgilga.ids.mqtt.MqttService;
import pl.miloszgilga.ids.mqtt.topic.MqttAppOutboundTopic;

public class DoorbellRingHandler implements WsMessageHandler {
    private final MqttService mqttService;
    private final WsSessionRegistry wsSessionRegistry;

    public DoorbellRingHandler(MqttService mqttService, WsSessionRegistry wsSessionRegistry) {
        this.mqttService = mqttService;
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public OpCode getOpCode() {
        return AppOpCode.DOORBELL_MANUALLY_RING;
    }

    @Override
    public void handle(String sessionId, JsonObject data) throws Exception {
        mqttService.publish(MqttAppOutboundTopic.DOORBELL_RING, MqttQoS.AT_LEAST_ONCE, false);
        wsSessionRegistry.broadcast(getOpCode());
    }
}
