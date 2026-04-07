#include "env_sensor.h"

#include "ds18b20.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "onewire_bus.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "ENV_SENSOR";

static onewire_bus_handle_t s_bus_handle = NULL;
static ds18b20_device_handle_t s_ds18b20_handle = NULL;
static bool s_is_initialized = false;

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t env_sensor_init(void)
{
    if (s_is_initialized) return ESP_OK;

    ESP_LOGI(TAG, "initializing 1-Wire bus on GPIO %d", ONEWIRE_BUS_GPIO);

    // bus Configuration
    onewire_bus_config_t bus_config = {
        .bus_gpio_num = ONEWIRE_BUS_GPIO,
    };
    // rmt configuration for 1-Wire
    onewire_bus_rmt_config_t rmt_config = {
        .max_rx_bytes = 10, 
    };
    esp_err_t err = onewire_new_bus_rmt(&bus_config, &rmt_config, &s_bus_handle);
    if (err != ESP_OK) return err;

    onewire_device_iter_handle_t iter = NULL;
    onewire_device_t next_onewire_device;

    err = onewire_new_device_iter(s_bus_handle, &iter);
    if (err != ESP_OK) return err;

    bool found = false;
    while (onewire_device_iter_get_next(iter, &next_onewire_device) == ESP_OK)
    {
        ds18b20_config_t ds_cfg = {};
        if (ds18b20_new_device_from_enumeration(&next_onewire_device, &ds_cfg, &s_ds18b20_handle) == ESP_OK)
        {
            uint64_t address;
            ds18b20_get_device_address(s_ds18b20_handle, &address);
            ESP_LOGI(TAG, "found DS18B20, address: %016llX", address);
            found = true;
            break;
        }
    }
    onewire_del_device_iter(iter);
    if (!found)
    {
        ESP_LOGE(TAG, "no DS18B20 device found on the bus");
        return ESP_ERR_NOT_FOUND;
    }
    s_is_initialized = true;
    ESP_LOGI(TAG, "env sensor ds18b20 initialized successfully");

    return ESP_OK;
}

esp_err_t env_sensor_read_temperature(float *out_temp_c)
{
    if (!s_is_initialized || s_ds18b20_handle == NULL || out_temp_c == NULL) return ESP_ERR_INVALID_STATE;

    esp_err_t err = ds18b20_trigger_temperature_conversion(s_ds18b20_handle);
    if (err != ESP_OK) return err;

    vTaskDelay(pdMS_TO_TICKS(800));
    return ds18b20_get_temperature(s_ds18b20_handle, out_temp_c);
}
