idf_component_register(
    SRCS
        "cyclic_task.c"

    INCLUDE_DIRS
        "include"

    REQUIRES
        cjson
        freertos
        env_sensor
        mqtt_registry
)
