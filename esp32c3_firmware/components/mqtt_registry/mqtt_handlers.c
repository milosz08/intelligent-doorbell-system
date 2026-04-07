#include "mqtt_handlers.h"
#include "app_state.h"

#include <stdbool.h>

#include "esp_log.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MQTT_HANDLERS";

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t mqtt_handler_doorbell_mode_set(const cJSON *args)
{
    if (args == NULL) return ESP_ERR_INVALID_ARG;
  
    cJSON *silent_item = cJSON_GetObjectItem(args, "silent");
    if (!cJSON_IsBool(silent_item))
    {
        ESP_LOGW(TAG, "missing or invalid 'silent' boolean parameter in payload");
        return ESP_ERR_INVALID_ARG;
    }
    bool is_silent = cJSON_IsTrue(silent_item);
    app_state_set_doorbell_silent(is_silent);
    return ESP_OK;
}
