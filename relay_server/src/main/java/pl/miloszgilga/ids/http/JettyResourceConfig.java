package pl.miloszgilga.ids.http;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyResourceConfig extends ResourceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(JettyResourceConfig.class);

    JettyResourceConfig(Set<Object> resources) {
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        for (final Object resource : resources) {
            register(resource);
            LOG.debug("Registered {} jetty resource", resource.getClass().getName());
        }
    }
}
