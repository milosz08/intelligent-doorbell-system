package pl.miloszgilga.ids.mqtt.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.mqtt.MqttMessageHandler;
import pl.miloszgilga.ids.mqtt.topic.MqttAppInboundTopic;
import pl.miloszgilga.ids.mqtt.topic.MqttInboundTopic;

public class MqttDoorbellOnRingEventHandler implements MqttMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MqttDoorbellOnRingEventHandler.class);

    @Override
    public MqttInboundTopic getTopic() {
        return MqttAppInboundTopic.DOORBELL_ON_RING;
    }

    @Override
    public void handle(JsonObject jsonPayload) {
        // TODO
        LOG.info("on ring event");
    }
}
