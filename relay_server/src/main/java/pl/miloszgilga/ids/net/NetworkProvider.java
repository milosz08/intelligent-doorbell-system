package pl.miloszgilga.ids.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.CriticalException;

public class NetworkProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkProvider.class);

    private static final String[] FORBIDDEN_NAMES = { "docker", "veth", "wsl" };
    private static final String[] FORBIDDEN_DISPLAY_NAMES = { "virtual", "vmware", "hyper-v" };

    private InetAddress cachedAddress = null;

    public InetAddress getLanInetAddress() {
        if (cachedAddress != null) {
            LOG.debug("Returning cached IP address: {}", cachedAddress.getHostAddress());
            return cachedAddress;
        }
        try {
            LOG.debug("Starting network interfaces scan for LAN address...");
            cachedAddress = scanForLanAddress();
            if (cachedAddress == null) {
                cachedAddress = getFallbackLocalHost();
            }
            return cachedAddress;
        } catch (SocketException ex) {
            throw new CriticalException(ex.getMessage(), ex);
        }
    }

    private InetAddress scanForLanAddress() throws SocketException {
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = interfaces.nextElement();
            if (isInterfaceEligible(networkInterface)) {
                final InetAddress validAddress = findValidIpv4Address(networkInterface);
                if (validAddress != null) {
                    return validAddress;
                }
            }
        }
        return null;
    }

    private boolean isInterfaceEligible(NetworkInterface networkInterface) throws SocketException {
        final String name = networkInterface.getName().toLowerCase();
        final String displayName = networkInterface.getDisplayName().toLowerCase();
        if (!networkInterface.isUp()) {
            LOG.trace("Skipping interface '{}' - it is down", name);
            return false;
        }
        if (networkInterface.isLoopback()) {
            LOG.trace("Skipping interface '{}' - it is a loopback", name);
            return false;
        }
        if (networkInterface.isVirtual()) {
            LOG.trace("Skipping interface '{}' - it is marked as virtual", name);
            return false;
        }
        final boolean isForbiddenName = Arrays.stream(FORBIDDEN_NAMES).anyMatch(name::contains);
        final boolean isForbiddenDisplayName = Arrays.stream(FORBIDDEN_DISPLAY_NAMES).anyMatch(displayName::contains);
        if (isForbiddenName || isForbiddenDisplayName) {
            LOG.debug("Skipping interface '{}' ('{}') - it is blacklisted", name, displayName);
            return false;
        }
        LOG.trace("Checking addresses for potentially valid interface: '{}' ('{}')", name, displayName);
        return true;
    }

    private InetAddress findValidIpv4Address(NetworkInterface networkInterface) {
        final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            final InetAddress addr = addresses.nextElement();
            if (addr instanceof Inet4Address && !addr.isLinkLocalAddress()) {
                LOG.info("Found valid LAN IPv4 address: {} (on interface '{}')", addr.getHostAddress(),
                        networkInterface.getName());
                return addr;
            } else {
                LOG.trace("Rejecting address {} - not ipv4 or is link-local", addr.getHostAddress());
            }
        }
        return null;
    }

    private InetAddress getFallbackLocalHost() {
        LOG.warn("No valid LAN address found on network interfaces, attempting to get LocalHost...");
        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            LOG.info("Fallback to LocalHost address: {}", localHost.getHostAddress());
            return localHost;
        } catch (Exception ex) {
            LOG.error("Failed to get fallback localhost address: {}", ex.getMessage());
            return null;
        }
    }
}
