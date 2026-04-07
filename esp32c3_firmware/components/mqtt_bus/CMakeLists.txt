idf_component_register(
    SRCS
        "mqtt_bus.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        json
        mqtt
        mbedtls
        esp_hw_support
)
