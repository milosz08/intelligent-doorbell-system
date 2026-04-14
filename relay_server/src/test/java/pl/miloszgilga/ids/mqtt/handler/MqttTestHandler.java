package pl.miloszgilga.ids.mqtt.handler;

import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.mqtt.MqttMessageHandler;
import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;
import pl.miloszgilga.ids.mqtt.topic.MqttTestInboundTopic;

public class MqttTestHandler implements MqttMessageHandler {
    private final CountDownLatch latch;
    private JsonObject lastReceivedJson;

    public MqttTestHandler(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public MqttInboundTopic getTopic() {
        return MqttTestInboundTopic.TEST_TOPIC;
    }

    @Override
    public void handle(JsonObject jsonPayload) {
        lastReceivedJson = jsonPayload;
        latch.countDown();
    }

    public JsonObject getLastReceivedJson() {
        return lastReceivedJson;
    }
}
