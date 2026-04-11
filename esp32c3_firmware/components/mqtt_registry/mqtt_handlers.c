#include "mqtt_handlers.h"
#include "app_state.h"
#include "doorbell_ctrl.h"
#include "sys_ind.h"

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

esp_err_t mqtt_handler_doorbell_ring(const cJSON *args)
{
    bool is_silent = app_state_get_doorbell_silent();
    if (is_silent)
    {
        ESP_LOGI(TAG, "doorbell is in silent mode, skip turning it on");
        return ESP_OK;
    }
    sys_ind_status_blink_normal();
    doorbell_ctrl_trigger_chime();
    return ESP_OK;
}
