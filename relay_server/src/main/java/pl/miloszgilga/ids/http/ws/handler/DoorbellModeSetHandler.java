package pl.miloszgilga.ids.http.ws.handler;

import com.google.gson.JsonObject;

import io.netty.handler.codec.mqtt.MqttQoS;
import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.http.ws.op.OpCode;
import pl.miloszgilga.ids.mqtt.MqttService;
import pl.miloszgilga.ids.mqtt.topic.MqttAppOutboundTopic;

public class DoorbellModeSetHandler implements WsMessageHandler {
    private final MqttService mqttService;
    private final WsSessionRegistry wsSessionRegistry;

    public DoorbellModeSetHandler(MqttService mqttService, WsSessionRegistry wsSessionRegistry) {
        this.mqttService = mqttService;
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public OpCode getOpCode() {
        return AppOpCode.DOORBELL_MODE_SET;
    }

    @Override
    public void handle(String sessionId, JsonObject data) throws Exception {
        mqttService.publish(MqttAppOutboundTopic.DOORBELL_MODE_SET, data, MqttQoS.AT_LEAST_ONCE, true);
        wsSessionRegistry.broadcast(getOpCode());
    }
}
