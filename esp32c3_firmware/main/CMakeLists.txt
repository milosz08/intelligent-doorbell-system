idf_component_register(
  SRCS
    "main.c"

  INCLUDE_DIRS
    "."

  REQUIRES
    log
    esp_driver_gpio
    freertos
    eth_w5500
)
