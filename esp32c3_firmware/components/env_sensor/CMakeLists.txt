idf_component_register(
    SRCS
        "env_sensor.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        ds18b20
        esp_driver_gpio
        esp_timer
        onewire_bus
)
