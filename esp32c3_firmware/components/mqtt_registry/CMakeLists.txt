idf_component_register(
    SRCS
        "mqtt_handlers.c"
        "mqtt_publishers.c"
        "mqtt_registry.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        cjson
        app_state
        doorbell_ctrl
        mqtt_bus
        sys_ind
)
