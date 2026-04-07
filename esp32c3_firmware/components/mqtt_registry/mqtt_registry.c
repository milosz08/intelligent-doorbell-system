#include "mqtt_registry.h"
#include "mqtt_bus.h"
#include "mqtt_handlers.h"
#include "mqtt_topics.h"

#include "esp_log.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MQTT_REGISTRY";

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t mqtt_registry_init(void)
{
  ESP_LOGI(TAG, "registering MQTT topic handlers");
  esp_err_t err;

  err = mqtt_bus_register_topic(MQTT_TOPIC_DOORBELL_MODE_SET, mqtt_handler_doorbell_mode_set);
  if (err != ESP_OK) return err;

  ESP_LOGI(TAG, "all mqtt topics registered successfully");
  return ESP_OK;
}
