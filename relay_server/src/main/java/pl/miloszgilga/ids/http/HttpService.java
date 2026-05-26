package pl.miloszgilga.ids.http;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ResourceServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.ComponentLifecycle;
import pl.miloszgilga.ids.CriticalException;
import pl.miloszgilga.ids.ThrowingRunnable;
import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.http.ws.JettyWsCreator;
import pl.miloszgilga.ids.http.ws.WsRouter;
import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.handler.WsMessageHandler;
import pl.miloszgilga.ids.security.Permission;
import pl.miloszgilga.ids.security.PermissionManager;

public class HttpService implements ComponentLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

    private final int port;
    private final SessionDao sessionDao;
    private final WsRouter wsRouter;
    private final WsSessionRegistry wsSessionRegistry;
    private final PermissionManager<Permission> permissionManager;
    private final Set<Object> resources;

    private Server server;
    private ServerConnector connector;

    private HttpService(Builder builder) {
        port = builder.port;
        sessionDao = builder.sessionDao;
        wsSessionRegistry = builder.wsSessionRegistry;
        permissionManager = builder.permissionManager;
        wsRouter = builder.wsRouter;
        resources = builder.resources;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void init() {
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("jetty-vt");
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server = new Server(threadPool);
        connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);

        final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.setShowStacks(false);
        errorHandler.setShowMessageInTitle(false);
        context.setErrorHandler(errorHandler);

        // static files
        final ServletHolder staticHolder = new ServletHolder("static", ResourceServlet.class);
        staticHolder.setInitParameter("baseResource",
                ResourceFactory.root().newClassLoaderResource("static").toString());
        staticHolder.setInitParameter("dirAllowed", "false");
        context.addServlet(staticHolder, "/static/*");

        // websocket
        final WebSocketUpgradeHandler wsHandler = WebSocketUpgradeHandler
                .from(server, container -> {
                    container.setIdleTimeout(Duration.ofMinutes(10));
                    container.setMaxTextMessageSize(128 * 1024);
                    container.addMapping("/v1", new JettyWsCreator(
                            sessionDao,
                            wsRouter,
                            wsSessionRegistry,
                            permissionManager));
                });
        wsHandler.setHandler(context);
        server.setHandler(wsHandler);

        final JettyResourceConfig resourceConfig = new JettyResourceConfig(resources);
        final ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));

        context.addServlet(jerseyServlet, "/*");
        try {
            LOG.info("HTTP server starting on port {}...", port);
            server.start();
            LOG.info("HTTP server is up and running");
        } catch (Exception ex) {
            throw new CriticalException(ex.getMessage(), ex);
        }
    }

    @Override
    public void close() throws IOException {
        connector.close();
        Utils.closeQuietly((ThrowingRunnable) () -> server.stop());
        LOG.info("Connector and HTTP server was successfully closed");
    }

    public static class Builder {
        private int port;
        private SessionDao sessionDao;
        private WsSessionRegistry wsSessionRegistry;
        private PermissionManager<Permission> permissionManager;
        private final WsRouter wsRouter = new WsRouter();
        private final Set<Object> resources = new HashSet<>();

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder sessionDao(SessionDao sessionDao) {
            this.sessionDao = sessionDao;
            return this;
        }

        public Builder wsSessionRegistry(WsSessionRegistry wsSessionRegistry) {
            this.wsSessionRegistry = wsSessionRegistry;
            return this;
        }

        public Builder permissionManager(PermissionManager<Permission> permissionManager) {
            this.permissionManager = permissionManager;
            return this;
        }

        public Builder addResource(Object resource) {
            resources.add(resource);
            return this;
        }

        public Builder addWsRouter(WsMessageHandler messageHandler) {
            wsRouter.registerHandler(messageHandler);
            return this;
        }

        public HttpService build() {
            return new HttpService(this);
        }
    }
}
