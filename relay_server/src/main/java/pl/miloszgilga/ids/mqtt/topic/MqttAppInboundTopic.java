package pl.miloszgilga.ids.mqtt.topic;

public enum MqttAppInboundTopic implements MqttInboundTopic {
    DOORBELL_ON_RING("ids/doorbell/on/ring"),
    ENV_STATUS("ids/env/status"),
    ;

    private final String path;

    MqttAppInboundTopic(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
