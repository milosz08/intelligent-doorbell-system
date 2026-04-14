package pl.miloszgilga.ids;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.mqtt.MqttConnectionHandler;
import pl.miloszgilga.ids.mqtt.MqttService;
import pl.miloszgilga.ids.mqtt.handler.MqttDoorbellOnRingEventHandler;
import pl.miloszgilga.ids.mqtt.handler.MqttEnvStatusHandler;
import pl.miloszgilga.ids.net.MdnsService;
import pl.miloszgilga.ids.net.NetworkProvider;

class RelayServerApplication implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(RelayServerApplication.class);

    private MqttService mqttService;
    private MdnsService mdnsService;

    private void start() {
        final AppConfig appConfig = new AppConfig();
        try {
            final NetworkProvider networkProvider = new NetworkProvider();

            mqttService = new MqttService(
                    List.of(
                            new MqttDoorbellOnRingEventHandler(),
                            new MqttEnvStatusHandler()),
                    new MqttConnectionHandler(),
                    appConfig.getAsStr(AppConfig.Prop.MQTT_SALT),
                    appConfig.getAsInt(AppConfig.Prop.MQTT_PORT),
                    appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_NAME));
            mqttService.start();

            mdnsService = new MdnsService(
                    networkProvider.getLanInetAddress(),
                    appConfig.getAsInt(AppConfig.Prop.MQTT_PORT),
                    appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_NAME),
                    appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_DESCRIPTION));
            mdnsService.init();

        } catch (CriticalException ex) {
            LOG.error("CRITICAL EXCEPTION: " + ex.getMessage(), ex);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        final RelayServerApplication relayServerApplication = new RelayServerApplication();
        Runtime.getRuntime().addShutdownHook(new Thread(relayServerApplication));
        relayServerApplication.start();
    }

    @Override
    public void run() {
        Utils.closeQuietly(mqttService);
        Utils.closeQuietly(mdnsService);
    }
}
