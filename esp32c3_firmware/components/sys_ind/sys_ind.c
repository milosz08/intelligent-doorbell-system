#include "sys_ind.h"

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/timers.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "SYS_IND";

typedef enum {
    MODE_OFF = 0,
    MODE_NORMAL,
    MODE_ERROR_TRIPLE,
    MODE_CRITICAL,
} status_led_mode_t;

static TimerHandle_t act_led_timer = NULL;
static volatile status_led_mode_t current_mode = MODE_OFF;

static void sys_ind_led_set_state(int gpio_num, bool active)
{
    gpio_set_level(gpio_num, active ? 0 : 1);
}

static void turn_off_led_callback(TimerHandle_t xTimer)
{
    sys_ind_led_set_state(ETH_ACTIVITY_YELLOW_LED_PIN, false);
}

static void perform_blink(int count, int speed_ms)
{
    for (int i = 0; i < count; i++)
    {
        sys_ind_led_set_state(STATUS_BLUE_LED_PIN, true);
        vTaskDelay(pdMS_TO_TICKS(speed_ms));
        sys_ind_led_set_state(STATUS_BLUE_LED_PIN, false);
        vTaskDelay(pdMS_TO_TICKS(speed_ms));
        if (current_mode == MODE_CRITICAL && count > 1) break;
    }
}

static void status_led_task(void *pvParameters)
{
    int blinks, speed;
    while (1)
    {
        blinks = 0;
        speed = 100;
        if (current_mode == MODE_CRITICAL)
        {
            blinks = 1;
            speed = 200;
        }
        else if (current_mode == MODE_ERROR_TRIPLE)
        {
            blinks = 3;
            speed = 150;
        }
        else if (current_mode == MODE_NORMAL)
        {
            blinks = 1;
            speed = 300;
        }
        if (blinks > 0)
        {
            perform_blink(blinks, speed);
            if (current_mode != MODE_CRITICAL) current_mode = MODE_OFF;
        }
        else
        {
            sys_ind_led_set_state(STATUS_BLUE_LED_PIN, false);
            vTaskDelay(pdMS_TO_TICKS(100));
        }
    }
}

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t sys_ind_init(void)
{
    gpio_config_t output_pins_config = {
        .pin_bit_mask   = (1ULL << ETH_LINK_GREEN_LED_PIN)
                        | (1ULL << ETH_ACTIVITY_YELLOW_LED_PIN)
                        | (1ULL << STATUS_BLUE_LED_PIN),
        .mode           = GPIO_MODE_OUTPUT,
        .pull_up_en     = GPIO_PULLUP_DISABLE,
        .pull_down_en   = GPIO_PULLDOWN_DISABLE,
        .intr_type      = GPIO_INTR_DISABLE,
    };
    esp_err_t err = gpio_config(&output_pins_config);
    if (err != ESP_OK) return err;

    // turn off leds
    gpio_set_level(ETH_LINK_GREEN_LED_PIN, 1);
    gpio_set_level(ETH_ACTIVITY_YELLOW_LED_PIN, 1);
    gpio_set_level(STATUS_BLUE_LED_PIN, 1);

    act_led_timer = xTimerCreate("act_timer", pdMS_TO_TICKS(LED_HOLD_TIME_MS), pdFALSE,
                                 (void *)ETH_ACTIVITY_YELLOW_LED_PIN, turn_off_led_callback);

    if (act_led_timer == NULL) return ESP_ERR_NO_MEM;

    BaseType_t task_created = xTaskCreate(status_led_task, "status_led_task", 2048, NULL, 5, NULL);
    if (task_created != pdPASS)
    {
        ESP_LOGE(TAG, "failed to create status_led_task");
        return ESP_FAIL;
    }
    ESP_LOGI(TAG, "initialized system indicators");
    return ESP_OK;
}

void sys_ind_led_eth_set_link(bool on)
{
    sys_ind_led_set_state(ETH_LINK_GREEN_LED_PIN, on);
}

void sys_ind_led_eth_packet_activity(void)
{
    if (act_led_timer != NULL)
    {
        sys_ind_led_set_state(ETH_ACTIVITY_YELLOW_LED_PIN, true);
        xTimerReset(act_led_timer, 0);
    }
}

void sys_ind_status_blink_normal(void)
{
    if (current_mode == MODE_OFF) current_mode = MODE_NORMAL;
}

void sys_ind_status_error_triple(void)
{
    if (current_mode < MODE_CRITICAL) current_mode = MODE_ERROR_TRIPLE;
}

void sys_ind_status_critical_start(void)
{
    current_mode = MODE_CRITICAL;
}
