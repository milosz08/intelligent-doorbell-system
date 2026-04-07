#include "cyclic_task.h"
#include "env_sensor.h"
#include "mqtt_publishers.h"

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "CYCLIC_TASK";

static void system_cyclic_task(void *pvParameters)
{
    ESP_LOGI(TAG, "cyclic task started, interval: %d ms", CYCLIC_TASK_INTERVAL_MS);
    while (1)
    {
        float current_temp = 0.0f;
        if (env_sensor_read_temperature(&current_temp) == ESP_OK) mqtt_publish_env_status(current_temp);
        else ESP_LOGD(TAG, "skipping temperature publish due to read error or uninitialized sensor");

        vTaskDelay(pdMS_TO_TICKS(CYCLIC_TASK_INTERVAL_MS));
    }
}

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t cyclic_task_init(void)
{
    BaseType_t res = xTaskCreate(system_cyclic_task, "system_cyclic_task", 4096, NULL, 3, NULL);
    if (res != pdPASS)
    {
        ESP_LOGE(TAG, "failed to create cyclic task");
        return ESP_ERR_NO_MEM;
    }
    return ESP_OK;
}
