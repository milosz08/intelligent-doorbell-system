package pl.miloszgilga.ids;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import pl.miloszgilga.ids.db.DbConnectionPool;
import pl.miloszgilga.ids.db.ExpiredSessionRemoval;
import pl.miloszgilga.ids.db.PasswordManager;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.jdbc.JdbcSessionDao;
import pl.miloszgilga.ids.db.jdbc.JdbcUserDao;
import pl.miloszgilga.ids.http.GlobalExceptionMapper;
import pl.miloszgilga.ids.http.HttpService;
import pl.miloszgilga.ids.http.api.AuthFilter;
import pl.miloszgilga.ids.http.api.AuthResource;
import pl.miloszgilga.ids.http.html.AuthViewFilter;
import pl.miloszgilga.ids.http.html.DashboardViewResource;
import pl.miloszgilga.ids.http.html.GuestViewFilter;
import pl.miloszgilga.ids.http.html.LoginViewResource;
import pl.miloszgilga.ids.http.html.SessionRefreshViewResponseFilter;
import pl.miloszgilga.ids.http.template.TemplateEngine;
import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.handler.DoorbellModeSetHandler;
import pl.miloszgilga.ids.http.ws.handler.DoorbellRingHandler;
import pl.miloszgilga.ids.http.ws.handler.HeartbeatHandler;
import pl.miloszgilga.ids.mqtt.MqttConnectionHandler;
import pl.miloszgilga.ids.mqtt.MqttService;
import pl.miloszgilga.ids.mqtt.handler.MqttDoorbellOnRingEventHandler;
import pl.miloszgilga.ids.mqtt.handler.MqttEnvStatusHandler;
import pl.miloszgilga.ids.mqtt.handler.MqttOnDoorbellClientRingHandler;
import pl.miloszgilga.ids.mqtt.handler.MqttOnDoorbellModeSetHandler;
import pl.miloszgilga.ids.net.MdnsService;
import pl.miloszgilga.ids.net.NetworkProvider;

class RelayServerApplication implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(RelayServerApplication.class);

    private DbConnectionPool dbConnectionPool;
    private ExpiredSessionRemoval expiredSessionRemoval;
    private MqttService mqttService;
    private MdnsService mdnsService;
    private HttpService httpService;

    private void start() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final AppConfig appConfig = new AppConfig();
        try {
            final NetworkProvider networkProvider = new NetworkProvider();

            dbConnectionPool = DbConnectionPool.builder()
                    .dbName(appConfig.getAsStr(AppConfig.Prop.DB_PATH))
                    .maximumPoolSize(appConfig.getAsInt(AppConfig.Prop.DB_POOL_SIZE))
                    .build();
            dbConnectionPool.init();

            final UserDao userDao = new JdbcUserDao(dbConnectionPool);
            final SessionDao sessionDao = new JdbcSessionDao(dbConnectionPool);

            userDao.init();
            sessionDao.init();

            final PasswordManager passwordManager = PasswordManager.builder()
                    .userDao(userDao)
                    .username(appConfig.getAsStr(AppConfig.Prop.ADMIN_USERNAME))
                    .passwordLength(appConfig.getAsInt(AppConfig.Prop.ADMIN_PASSWORD_LENGTH))
                    .hashStrength(appConfig.getAsInt(AppConfig.Prop.ADMIN_PASSWORD_HASH_STRENGTH))
                    .build();
            passwordManager.init();

            expiredSessionRemoval = ExpiredSessionRemoval.builder()
                    .sessionDao(sessionDao)
                    .intervalSec(appConfig.getAsInt(AppConfig.Prop.SESSION_CLEAR_INTERVAL_SEC))
                    .build();
            expiredSessionRemoval.init();

            final WsSessionRegistry wsSessionRegistry = new WsSessionRegistry();

            mqttService = MqttService.builder()
                    .port(appConfig.getAsInt(AppConfig.Prop.MQTT_PORT))
                    .brokerClientId(appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_NAME))
                    .authSalt(appConfig.getAsStr(AppConfig.Prop.MQTT_SALT))
                    .connectionCallback(new MqttConnectionHandler())
                    .addMessageHandler(new MqttDoorbellOnRingEventHandler(wsSessionRegistry))
                    .addMessageHandler(new MqttEnvStatusHandler(wsSessionRegistry))
                    .addMessageHandler(new MqttOnDoorbellClientRingHandler(wsSessionRegistry))
                    .addMessageHandler(new MqttOnDoorbellModeSetHandler(wsSessionRegistry))
                    .build();
            mqttService.init();

            mdnsService = MdnsService.builder()
                    .address(networkProvider.getLanInetAddress())
                    .port(appConfig.getAsInt(AppConfig.Prop.MQTT_PORT))
                    .serviceName(appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_NAME))
                    .serviceDescription(appConfig.getAsStr(AppConfig.Prop.MDNS_SERVICE_DESCRIPTION))
                    .build();
            mdnsService.init();

            final TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.init();

            httpService = HttpService.builder()
                    .port(appConfig.getAsInt(AppConfig.Prop.HTTP_PORT))
                    .sessionDao(sessionDao)
                    .wsSessionRegistry(wsSessionRegistry)
                    // api
                    .addResource(new AuthResource(
                            passwordManager,
                            userDao,
                            sessionDao,
                            appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC)))
                    .addResource(new AuthFilter(sessionDao,
                            appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC)))
                    .addResource(new GlobalExceptionMapper())
                    // html views
                    .addResource(new DashboardViewResource(templateEngine, userDao, sessionDao))
                    .addResource(new LoginViewResource(
                            templateEngine,
                            userDao,
                            sessionDao,
                            passwordManager,
                            appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC)))
                    .addResource(new AuthViewFilter(sessionDao,
                            appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC)))
                    .addResource(new GuestViewFilter())
                    .addResource(new SessionRefreshViewResponseFilter())
                    // websocket
                    .addWsRouter(new DoorbellModeSetHandler(mqttService, wsSessionRegistry))
                    .addWsRouter(new DoorbellRingHandler(mqttService, wsSessionRegistry))
                    .addWsRouter(new HeartbeatHandler(wsSessionRegistry))
                    .build();
            httpService.init();

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
        Utils.closeQuietly(httpService);
        Utils.closeQuietly(mdnsService);
        Utils.closeQuietly(mqttService);
        Utils.closeQuietly(expiredSessionRemoval);
        Utils.closeQuietly(dbConnectionPool);
    }
}
