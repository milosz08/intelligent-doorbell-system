idf_component_register(
    SRCS
        "eth_w5500.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        esp_driver_gpio
        esp_driver_spi
        esp_eth
        esp_event
        esp_netif
        w5500
)
