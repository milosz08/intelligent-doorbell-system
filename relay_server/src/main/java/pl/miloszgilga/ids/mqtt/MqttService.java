package pl.miloszgilga.ids.mqtt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import pl.miloszgilga.ids.ComponentLifecycle;
import pl.miloszgilga.ids.CriticalException;
import pl.miloszgilga.ids.mqtt.topic.MqttOutboundTopic;

public class MqttService implements ComponentLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(MqttService.class);

    private final Server mqttBroker;
    private final ExecutorService executorService;
    private final IConfig brokerConfig;
    private final MqttPublishHandler eventInterceptor;
    private final MqttEspAuthenticator authenticator;
    private final String brokerClientId;

    private MqttService(Builder builder) {
        mqttBroker = new Server();
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        brokerConfig = createConfig(builder.port);
        authenticator = new MqttEspAuthenticator(builder.authSalt);
        eventInterceptor = new MqttPublishHandler(
                builder.messageHandlers,
                executorService,
                builder.connectionCallback);
        brokerClientId = builder.brokerClientId;
        LOG.info("MqttService initialized with {} message handlers", builder.messageHandlers.size());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void init() {
        LOG.info("Starting MQTT broker on port {}...", brokerConfig.getProperty("port"));
        try {
            mqttBroker.startServer(
                    brokerConfig,
                    Collections.singletonList(eventInterceptor),
                    null,
                    authenticator,
                    null);
            LOG.info("MQTT broker is up and running");
        } catch (IOException ex) {
            throw new CriticalException(ex.getMessage(), ex);
        }
    }

    public void publish(MqttOutboundTopic topic, JsonObject jsonPayload, MqttQoS qos, boolean retained) {
        final ByteBuf payloadBuf = Unpooled.copiedBuffer(jsonPayload.toString(), StandardCharsets.UTF_8);
        internalPublish(topic.getPath(), payloadBuf, qos, retained);
    }

    public void publish(MqttOutboundTopic topic, MqttQoS qos, boolean retained) {
        internalPublish(topic.getPath(), Unpooled.EMPTY_BUFFER, qos, retained);
    }

    @Override
    public void close() throws IOException {
        mqttBroker.stopServer();
        executorService.shutdown();
        LOG.info("Virtual thread executor and MQTT broker was successfully closed");
    }

    private void internalPublish(String path, ByteBuf payload, MqttQoS qos, boolean retained) {
        LOG.debug("Internal publish to topic: {} [qos: {}, retain: {}, bytes: {}]", path, qos, retained,
                payload.readableBytes());
        final MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                false,
                qos,
                retained,
                0);
        final MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(path, 0);
        final MqttPublishMessage message = new MqttPublishMessage(fixedHeader, varHeader, payload);
        mqttBroker.internalPublish(message, brokerClientId);
    }

    private IConfig createConfig(int port) {
        final Properties props = new Properties();
        props.setProperty("port", String.valueOf(port));
        props.setProperty("host", "0.0.0.0");
        props.setProperty("allow_anonymous", "false");
        return new MemoryConfig(props);
    }

    public static class Builder {
        private int port;
        private String brokerClientId;
        private String authSalt;
        private MqttConnectionCallback connectionCallback;
        private final Set<MqttMessageHandler> messageHandlers = new HashSet<>();

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder brokerClientId(String brokerClientId) {
            this.brokerClientId = brokerClientId;
            return this;
        }

        public Builder authSalt(String authSalt) {
            this.authSalt = authSalt;
            return this;
        }

        public Builder connectionCallback(MqttConnectionCallback connectionCallback) {
            this.connectionCallback = connectionCallback;
            return this;
        }

        public Builder addMessageHandler(MqttMessageHandler messageHandler) {
            messageHandlers.add(messageHandler);
            return this;
        }

        public MqttService build() {
            return new MqttService(this);
        }
    }
}
