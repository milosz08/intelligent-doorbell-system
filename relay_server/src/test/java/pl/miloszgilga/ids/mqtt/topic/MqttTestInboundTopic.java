package pl.miloszgilga.ids.mqtt.topic;

public enum MqttTestInboundTopic implements MqttInboundTopic {
    TEST_TOPIC("ids/test/topic"),
    ;

    private final String path;

    MqttTestInboundTopic(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
