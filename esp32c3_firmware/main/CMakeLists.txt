idf_component_register(
    SRCS
        "main.c"

    INCLUDE_DIRS
        "."

    REQUIRES
        log
        app_state
        cyclic_task
        env_sensor
        eth_w5500
        mdns_service
        mqtt_bus
        mqtt_registry
)
