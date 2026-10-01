# Intelligent doorbell system

Intelligent Doorbell System is a flexible IoT platform built around a relay server, enabling device management from
anywhere in the world. Currently, the system is tailored to operate a smart doorbell, but its architecture allows for
easy integration of new devices and features in the future.

* **Installation & provisioning:** See [INSTALL](./INSTALL.md) for instructions on how to flash and set up the device.
* **Development:** See [CONTRIBUTING](./CONTRIBUTING.md) for setting up the environment and building artifacts.

## Table of content

* [System infrastructure](#system-infrastructure)
* [Hardware](#hardware)
* [Software](#software)
* [Hardware and software stack](#hardware-and-software-stack)
* [Author](#author)
* [License](#license)

## System infrastructure

<img src=".github/diagram/infra.svg" width="100%">

The client communicates with the local network via a secure tunnel (WSS), eliminating the need for a public IP address
and NAT rule configuration on the edge firewall. External traffic is received by a daemon on the edge server and routed
to a containerized intermediary node (Relay Server). This internal service manages the asynchronous message exchange
with a local MQTT broker. The IoT endpoint maintains a persistent TCP session with the broker, subscribing to command
topics and publishing its operational status. Ultimately, the microcontroller translates the received network packets
into physical logical state changes on GPIO pins, directly driving the relay module. Isolating the Relay Server as an
independent component enables the delegation of business logic, database persistence, and telemetry archiving outside
the embedded layer.

## Hardware

[Click to open schematic in PDF format](.github/schematic/esp32c3-driver.pdf)

<img src=".github/schematic/esp32c3-driver.svg" width="100%">

The physical hardware is built around the RISC-V based ESP32-C3 microcontroller, chosen to fit the tight space
constraints, and a W5500 Ethernet controller for highly stable wired connectivity. The circuit utilizes a dual-relay
setup: a mechanical relay with a normally closed contact manages the silent mode, while a solid-state relay triggers the
doorbell programmatically. Because the ESP logic operates at 3.3V and the relays require 5V, driving transistors are
used to provide the necessary voltage and current for reliable coil switching. Crucially, an optocoupler listening for
shorts on the 230V live line ensures the doorbell functions normally even if the IoT controller fails entirely. The 230V
zone is protected by a B10 circuit breaker, and an RC snubber with a varistor is installed directly before the doorbell
to suppress electrical arcs and destructive voltage spikes from inductive loads. The high-voltage side of the
optocoupler is protected by an additional 0.5A glass fuse. Device status is visually indicated by LEDs corresponding to
the Ethernet link, manual ringing, and programmatic ringing.   

### Standalone unit (boxed)

[TBD]

### Prototype with test bench

[TBD]

## Software

### ESP32C3 firmware

The firmware handles rapid local network communication, deliberately omitting internal intranet encryption to minimize
transmission latency, as internet-facing security is managed by a dedicated Cloudflare tunnel. Device-to-server
communication relies on the MQTT protocol, organized into distinct bus and registry components. Device authentication
with the relay server is performed using the MAC address and a password.

### Java relay server with embed MQTT broker and mDNS

The relay server is developed in Java, leveraging the lightweight Jetty HTTP server and Jersey for JAX-WS and JAX-RS
implementations to ensure a minimal footprint. Primary communication between IoT modules and the server is handled via
MQTT. Real-time communication between end clients and the server, including live dashboard updates, is powered by
WebSockets. To keep the payload overhead minimal, the WebSocket structure is deeply optimized using custom opcodes and
bitwise shifts. For database object mapping, MyBatis was selected over the heavier Hibernate framework due to the low
complexity of the system's entities and relationships. Additionally, an integrated mDNS server continuously broadcasts
the MQTT broker's address to all end devices on the local network, streamlining setup and removing the need to hardcode
the server's IP into every IoT module.

### Multi-platform client

[TBD]

### Provisioning tool (ESP32C3 flashing)

[TBD]

## Hardware and software stack

[TBD]

## Author

Created by Miłosz Gilga. If you have any questions about this project, send message:
[miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the GNU General Public License v3.0.
