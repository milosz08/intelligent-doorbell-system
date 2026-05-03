package pl.miloszgilga.ids.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.mqtt.handler.MqttTestHandler;

class MqttServiceIntegrationTest {
    private static final String TEST_CLIENT_ID = "PahoTestClient";
    private static final String TEST_SALT = "test-salt";

    private static MqttService mqttService;
    private static final int TEST_PORT = 11883;
    private static MqttTestHandler testHandler;
    private static CountDownLatch latch;

    @BeforeEach
    void startBroker() throws Exception {
        latch = new CountDownLatch(1);
        testHandler = new MqttTestHandler(latch);
        mqttService = MqttService.builder()
                .port(TEST_PORT)
                .brokerClientId("TestBroker")
                .authSalt(TEST_SALT)
                .addMessageHandler(testHandler)
                .build();
        mqttService.init();
    }

    @AfterEach
    void tearDown() {
        Utils.closeQuietly(mqttService);
    }

    @Test
    @DisplayName("publish message through network and verify handler logic")
    void shouldRouteAndProcessRealMessage() throws Exception {
        // arrange
        final String topic = "ids/test/topic";
        final String jsonPayload = "{\"temperature\": 22.5, \"sensor\": \"DHT22\"}";
        final MqttClient client = new MqttClient("tcp://localhost:" + TEST_PORT, "PahoTestClient");

        final String expectedHash = DigestUtils.sha256Hex(TEST_CLIENT_ID + TEST_SALT);
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(TEST_CLIENT_ID);
        options.setPassword(expectedHash.toCharArray());
        try {
            client.connect(options);
            final MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic, message);

            final boolean completed = latch.await(3, TimeUnit.SECONDS);
            assertTrue(completed, "Message should reach the handler within timeout");

            final JsonObject result = testHandler.getLastReceivedJson();
            assertNotNull(result);
            assertEquals(22.5, result.get("temperature").getAsDouble());
            assertEquals("DHT22", result.get("sensor").getAsString());
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
    }
}
