#include "app_state.h"

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "APP_STATE";

// global state variables
static bool s_is_silent_mode = false;

static app_state_doorbell_cb_t s_doorbell_callbacks[MAX_STATE_CALLBACKS];
static int s_doorbell_cb_count = 0;

static SemaphoreHandle_t s_state_mutex = NULL;

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t app_state_init(void)
{
    if (s_state_mutex != NULL) return ESP_OK;

    s_state_mutex = xSemaphoreCreateMutex();
    if (s_state_mutex == NULL) return ESP_ERR_NO_MEM;

    ESP_LOGI(TAG, "app state manager init");
    return ESP_OK;
}

esp_err_t app_state_register_doorbell_cb(app_state_doorbell_cb_t cb)
{
    if (s_doorbell_cb_count >= MAX_STATE_CALLBACKS)
    {
        ESP_LOGE(TAG, "doorbell callback registry full");
        return ESP_ERR_NO_MEM;
    }
    s_doorbell_callbacks[s_doorbell_cb_count] = cb;
    s_doorbell_cb_count++;

    return ESP_OK;
}

void app_state_set_doorbell_silent(bool is_silent)
{
    if (s_state_mutex == NULL) return;
    xSemaphoreTake(s_state_mutex, portMAX_DELAY);
    if (s_is_silent_mode != is_silent)
    {
        s_is_silent_mode = is_silent;
        ESP_LOGI(TAG, "doorbell silent mode set to: %d", is_silent);
        for (int i = 0; i < s_doorbell_cb_count; i++)
        {
            if (s_doorbell_callbacks[i] != NULL) s_doorbell_callbacks[i](s_is_silent_mode);
        }
    }
    xSemaphoreGive(s_state_mutex);
}

bool app_state_get_doorbell_silent(void)
{
    if (s_state_mutex == NULL) return false;
    xSemaphoreTake(s_state_mutex, portMAX_DELAY);
    bool state = s_is_silent_mode;
    xSemaphoreGive(s_state_mutex);
    return state;
}
