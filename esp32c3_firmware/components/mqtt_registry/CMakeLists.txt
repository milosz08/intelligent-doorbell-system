idf_component_register(
    SRCS
        "mqtt_handlers.c"
        "mqtt_publishers.c"
        "mqtt_registry.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        json
        app_state
        mqtt_bus
)
