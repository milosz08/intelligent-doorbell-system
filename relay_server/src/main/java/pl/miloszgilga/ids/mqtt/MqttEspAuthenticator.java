package pl.miloszgilga.ids.mqtt;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.broker.security.IAuthenticator;

class MqttEspAuthenticator implements IAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(MqttEspAuthenticator.class);
    private final String authSalt;

    MqttEspAuthenticator(String authSalt) {
        this.authSalt = authSalt;
    }

    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        if (username == null || password == null) {
            LOG.warn("Rejected connection for client '{}': missing username or password", clientId);
            return false;
        }
        final String expectedHash = DigestUtils.sha256Hex(username + authSalt);
        final String receivedPassword = new String(password, StandardCharsets.UTF_8);

        final boolean isValid = expectedHash.equalsIgnoreCase(receivedPassword);
        if (!isValid) {
            LOG.warn("Rejected connection for: {} (invalid password/hash)", username);
        } else {
            LOG.info("Client authenticated successfully: {}", username);
        }
        return isValid;
    }
}
