package pl.miloszgilga.ids;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppConfig {
    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
    private static final String PROPERTIES_FILE = "server.properties";

    private final Properties properties;

    AppConfig() {
        properties = new Properties();
        try (final InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(input);
        } catch (IOException ex) {
            LOG.warn("Unable to load file: {}, configuration will rely solely on environment variables",
                    PROPERTIES_FILE);
        }
    }

    private String resolveProperty(AppConfig.Prop prop) {
        final String envKey = prop.name();
        final String envValue = System.getenv(envKey);

        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        final String propKey = prop.key;
        final String propValue = properties.getProperty(propKey);

        if (propValue == null) {
            LOG.warn("Configuration key not found either in environment variables (as: {}) or in the " +
                    "properties file (as: {})", envKey, propKey);
        }
        return propValue;
    }

    String getAsStr(AppConfig.Prop prop) {
        return resolveProperty(prop);
    }

    long getAsLong(AppConfig.Prop prop) {
        return Utils.safetyParseLong(resolveProperty(prop), 0L);
    }

    int getAsInt(AppConfig.Prop prop) {
        return Utils.safetyParseInt(resolveProperty(prop), 0);
    }

    boolean getAsBoolean(AppConfig.Prop prop) {
        return resolveProperty(prop).equals("true");
    }

    // prop.name() -> key to environment path (ex. HTTP_PORT)
    // prop.key -> key to property in properties file (ex. http-port)
    enum Prop {
        HTTP_PORT("http-port"),
        ENABLE_HTML_TEMPLATES_CACHING("enable-html-templates-caching"),
        MQTT_PORT("mqtt-port"),
        MQTT_SALT("mqtt-salt"),
        MDNS_SERVICE_NAME("mdns-service-name"),
        MDNS_SERVICE_DESCRIPTION("mdns-service-description"),
        DB_PATH("db-path"),
        DB_POOL_SIZE("db-pool-size"),
        SESSION_TTL_SEC("session-ttl-sec"),
        SESSION_CLEAR_INTERVAL_SEC("session-clear-interval-sec"),
        ADMIN_USERNAME("admin-username"),
        ADMIN_PASSWORD_LENGTH("admin-password-length"),
        ADMIN_PASSWORD_HASH_STRENGTH("admin-password-hash-strength"),
        ;

        private final String key;

        Prop(String key) {
            this.key = key;
        }
    }
}
