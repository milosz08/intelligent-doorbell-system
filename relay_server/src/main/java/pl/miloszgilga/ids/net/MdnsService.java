package pl.miloszgilga.ids.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.CriticalException;

public class MdnsService implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MdnsService.class);

    private static final String SERVICE_TYPE = "_mqtt._tcp.local.";
    private static final String SCHEME = "mqtt";

    private final InetAddress address;
    private final int port;
    private final String serviceName;
    private final String serviceDescription;

    private JmDNS jmdns;

    public MdnsService(InetAddress address, int port, String serviceName, String serviceDescription) {
        this.address = address;
        this.port = port;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
    }

    public void init() {
        try {
            LOG.info("Initializing mDNS on IP address: {}", address.getHostAddress());
            final JmDNS jmdns = JmDNS.create(address);
            final ServiceInfo serviceInfo = ServiceInfo.create(
                    SERVICE_TYPE,
                    serviceName,
                    port,
                    serviceDescription);
            jmdns.registerService(serviceInfo);
            final Inet4Address[] ipv4Addresses = serviceInfo.getInet4Addresses();
            final String mqttUri = createMqttUri(ipv4Addresses, port);
            LOG.info("Successfully registered mDNS service: {}", mqttUri);
            this.jmdns = jmdns;
        } catch (IOException ex) {
            throw new CriticalException(ex.getMessage(), ex);
        }
    }

    public JmDNS getJmdns() {
        return jmdns;
    }

    private String createMqttUri(Inet4Address[] ipv4Addresses, int port) throws IOException {
        if (ipv4Addresses.length <= 0) {
            throw new IOException("No IPv4 addresses were resolved");
        }
        final String ipAddress = ipv4Addresses[0].getHostAddress();
        return SCHEME + "://" + ipAddress + ":" + port;
    }

    @Override
    public void close() throws IOException {
        if (jmdns == null) {
            return;
        }
        jmdns.unregisterAllServices();
        jmdns.close();
        LOG.info("mDNS service was successfully closed");
    }
}
