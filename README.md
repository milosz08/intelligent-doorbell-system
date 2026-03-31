# Intelligent door bell

This ESP32-C3 and W5500 Ethernet doorbell uses BC337 transistors in a Wired-OR setup to integrate a physical button with
digital logic. Two relays manage a 230V bell for triggering and silent mode blocking, while the hardware configuration
enables manual operation during an ESP32 system hang. Data travels via MQTT and Cloudflare Tunnel to a Java backend and
multiplatform mobile/desktop apps, supported by single-click OTA updates.

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

[TBD]

## Hardware

[Click to open schematic in PDF format](.github/schematic/esp32c3-driver.pdf)

<img src=".github/schematic/esp32c3-driver.svg" width="100%">

[TBD]

### Standalone unit (boxed)

[TBD]

### Prototype with test bench

[TBD]

## Software

### ESP32 firmware

[TBD]

### Multi-platform client

[TBD]

### Provisioning tool (certgen and ESP32 flashing)

[TBD]

## Hardware and software stack

[TBD]

## Author

Created by Miłosz Gilga. If you have any questions about this project, send message:
[miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the GNU General Public License v3.0.
