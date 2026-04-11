#include "doorbell_ctrl.h"

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/timers.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "DOORBELL_CTRL";

static doorbell_pressed_cb_t on_pressed_cb = NULL;
static TimerHandle_t ssr_timer = NULL;

static void set_ssrelay_state(bool on)
{
    gpio_set_level(DOORBELL_SSR_PIN, on ? 0 : 1);
}

static void set_relay_nc_state(bool on)
{
    gpio_set_level(DOORBELL_SILENT_RELAY_PIN, on ? DOORBELL_RELAY_ON : DOORBELL_RELAY_OFF);
}

static void ssr_turn_off_callback(TimerHandle_t xTimer)
{
    set_ssrelay_state(false);
}

static void ac_detect_task(void *pvParameters)
{
    int detect_counter = 0;
    bool is_ringing = false;
    while (1)
    {
        if (gpio_get_level(DOORBELL_AC_DETECT_PIN) == 0) detect_counter++;
        else if (detect_counter > 0) detect_counter--;

        if (detect_counter > 15) detect_counter = 15;

        if (detect_counter > 4 && !is_ringing)
        {
            is_ringing = true;
            ESP_LOGI(TAG, "230V detected, physical button pressed");
            if (on_pressed_cb) on_pressed_cb();
        }
        else if (detect_counter == 0 && is_ringing)
        {
            is_ringing = false;
        }
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t doorbell_ctrl_init(void)
{
    // relays config
    gpio_config_t out_conf = {
        .pin_bit_mask   = (1ULL << DOORBELL_SSR_PIN)
                        | (1ULL << DOORBELL_SILENT_RELAY_PIN),
        .mode           = GPIO_MODE_OUTPUT,
        .pull_up_en     = GPIO_PULLUP_DISABLE,
        .pull_down_en   = GPIO_PULLDOWN_DISABLE,
        .intr_type      = GPIO_INTR_DISABLE,
    };
    esp_err_t err = gpio_config(&out_conf);
    if (err != ESP_OK) return err;

    set_ssrelay_state(false);
    set_relay_nc_state(false);

    // transoptor config (AC)
    gpio_config_t in_conf = {
        .pin_bit_mask   = (1ULL << DOORBELL_AC_DETECT_PIN),
        .mode           = GPIO_MODE_INPUT,
        .pull_up_en     = GPIO_PULLUP_DISABLE, // disabled, because its physically 10k resisitor
        .pull_down_en   = GPIO_PULLDOWN_DISABLE,
        .intr_type      = GPIO_INTR_DISABLE,
    };
    err = gpio_config(&in_conf);
    if (err != ESP_OK) return err;

    ssr_timer = xTimerCreate("ssr_tmr", pdMS_TO_TICKS(DOORBELL_SSR_PULSE_MS), pdFALSE, NULL, ssr_turn_off_callback);
    if (ssr_timer == NULL) return ESP_ERR_NO_MEM;

    BaseType_t task_created = xTaskCreate(ac_detect_task, "ac_detect", 2048, NULL, 5, NULL);
    if (task_created != pdPASS)
    {
        ESP_LOGE(TAG, "failed to create AC detect task");
        return ESP_FAIL;
    }
    ESP_LOGI(TAG, "doorbell control initialized");
    return ESP_OK;
}

void doorbell_ctrl_set_callback(doorbell_pressed_cb_t cb)
{
    on_pressed_cb = cb;
}

void doorbell_ctrl_trigger_chime(void)
{
    if (ssr_timer == NULL) return;

    set_ssrelay_state(true);
    ESP_LOGI(TAG, "triggering chime (ssr on)");

    xTimerReset(ssr_timer, 0);
}

void doorbell_ctrl_set_silent_mode(bool silent)
{
    set_relay_nc_state(silent);
    ESP_LOGI(TAG, "silent mode: %s", silent ? "enabled" : "disabled");
}
