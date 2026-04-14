package pl.miloszgilga.ids.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttConnectionHandler implements MqttConnectionCallback {
    private static final Logger LOG = LoggerFactory.getLogger(MqttConnectionHandler.class);

    @Override
    public void onConnected(String clientId, String username) {
        LOG.info("Client CONNECTED, ID: {}, username: {}", clientId, username);
    }

    @Override
    public void onDisconnected(String clientId, boolean wasLost) {
        if (wasLost) {
            LOG.warn("Client CONNECTION LOST (timeout/error), ID: {}", clientId);
        } else {
            LOG.info("Client DISCONNECTED (gracefully), ID: {}", clientId);
        }
    }
}
