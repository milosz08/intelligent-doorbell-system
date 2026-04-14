package pl.miloszgilga.ids.mqtt;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;

public interface MqttMessageHandler {
    MqttInboundTopic getTopic();

    void handle(JsonObject jsonPayload);
}
