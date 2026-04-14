package pl.miloszgilga.ids.mqtt.topic;

public enum MqttAppOutboundTopic implements MqttOutboundTopic {
    DOORBELL_MODE_SET("ids/doorbell/mode/set"),
    DOORBELL_RING("ids/doorbell/ring"),
    ;

    private final String path;

    MqttAppOutboundTopic(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
