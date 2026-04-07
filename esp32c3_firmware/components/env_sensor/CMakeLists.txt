idf_component_register(
    SRCS
        "env_sensor.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        driver
        ds18b20
        esp_timer
        onewire_bus
)
