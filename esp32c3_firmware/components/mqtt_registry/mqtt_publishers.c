#include "mqtt_publishers.h"
#include "mqtt_bus.h"
#include "mqtt_topics.h"

#include <stdbool.h>

#include "cJSON.h"
#include "esp_log.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MQTT_PUBLISHERS";

// public api ----------------------------------------------------------------------------------------------------------

void mqtt_publish_doorbell_ring_event(void)
{
    bool success = mqtt_bus_publish(MQTT_TOPIC_DOORBELL_ON_RING, NULL, 1, 0);
    if (success) ESP_LOGD(TAG, "doorbell ring event published successfully");
    else ESP_LOGE(TAG, "failed to publish doorbell ring event");
}

void mqtt_publish_env_status(float temperature)
{
    cJSON *payload_args = cJSON_CreateObject();
    if (payload_args == NULL) 
    {
        ESP_LOGE(TAG, "failed to allocate json object");
        return;
    }
    cJSON_AddNumberToObject(payload_args, "temp", temperature);
    char *json_str = cJSON_PrintUnformatted(payload_args);
    if (json_str != NULL)
    {
        bool success = mqtt_bus_publish(MQTT_TOPIC_ENV_STATUS, json_str, 1, 1);
        if (success) ESP_LOGD(TAG, "telemetry event published successfully");
        else ESP_LOGW(TAG, "failed to publish telemetry event");
        free(json_str);
    } else ESP_LOGE(TAG, "failed to serialize json");
    cJSON_Delete(payload_args);
}

void mqtt_publish_on_doorbell_mode_set(bool is_silent)
{
    cJSON *payload_args = cJSON_CreateObject();
    if (payload_args == NULL) 
    {
        ESP_LOGE(TAG, "failed to allocate json object");
        return;
    }
    cJSON_AddBoolToObject(payload_args, "is_silent", is_silent);
    char *json_str = cJSON_PrintUnformatted(payload_args);
    if (json_str != NULL)
    {
        bool success = mqtt_bus_publish(MQTT_TOPIC_ON_DOORBELL_MODE_SET, json_str, 1, 1);
        if (success) ESP_LOGD(TAG, "doorbell mode set event published successfully");
        else ESP_LOGW(TAG, "failed to publish doorbell mode set event");
        free(json_str);
    } else ESP_LOGE(TAG, "failed to serialize json");
    cJSON_Delete(payload_args);
}

void mqtt_publish_on_doorbell_client_ring_event(void)
{
    bool success = mqtt_bus_publish(MQTT_TOPIC_ON_DOORBELL_RING, NULL, 1, 0);
    if (success) ESP_LOGD(TAG, "doorbell client ring event published successfully");
    else ESP_LOGE(TAG, "failed to publish doorbell client ring event");
}
