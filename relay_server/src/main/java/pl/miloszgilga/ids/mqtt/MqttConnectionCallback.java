package pl.miloszgilga.ids.mqtt;

public interface MqttConnectionCallback {
    void onConnected(String clientId, String username);

    void onDisconnected(String clientId, boolean wasLost);
}
