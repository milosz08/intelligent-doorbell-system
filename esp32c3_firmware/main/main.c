#include "eth_w5500.h"

#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_event.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/timers.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MAIN";

static TimerHandle_t act_led_timer = NULL;

#define PIN_ETH_LINK  GPIO_NUM_21
#define PIN_ETH_DATA  GPIO_NUM_20

#define CHECK_CRITICAL(x, msg) do { \
  esp_err_t err_rc = (x); \
  if (err_rc != ESP_OK) { \
    ESP_LOGE(TAG, msg); \
  } \
} while(0)

static void turn_off_led_callback(TimerHandle_t xTimer)
{
  gpio_set_level(PIN_ETH_DATA, 1);
}

static void link_state_changed(bool on)
{
  gpio_set_level(PIN_ETH_LINK, on ? 0 : 1);
}

static void packet_received(void)
{
  if (act_led_timer != NULL)
  {
    gpio_set_level(PIN_ETH_DATA, 0);
    xTimerReset(act_led_timer, 0);
  }
}

static void on_eth_boot_wait(bool linked)
{
  if (!linked)
  {
    ESP_LOGW(TAG, "not linked");
  }
  else
  {
    ESP_LOGI(TAG, "linked");
  }
  link_state_changed(linked);
}

// public api ----------------------------------------------------------------------------------------------------------

void app_main(void)
{
  CHECK_CRITICAL(esp_netif_init(), "Netif fail");
  CHECK_CRITICAL(esp_event_loop_create_default(), "EventLoop fail");
  CHECK_CRITICAL(gpio_install_isr_service(0), "GPIO ISR fail"); // enable interrupts

  gpio_reset_pin(PIN_ETH_LINK);
  gpio_reset_pin(PIN_ETH_DATA);

  gpio_set_direction(PIN_ETH_LINK, GPIO_MODE_OUTPUT);
  gpio_set_direction(PIN_ETH_DATA, GPIO_MODE_OUTPUT);

  gpio_set_level(PIN_ETH_LINK, 1);
  gpio_set_level(PIN_ETH_DATA, 1);

  act_led_timer = xTimerCreate("act_timer", pdMS_TO_TICKS(100), pdFALSE, (void *)0, turn_off_led_callback);

  eth_callbacks_t eth_callbacks = {
    .on_link_state_changed = link_state_changed,
    .on_packet_received = packet_received
  };
  CHECK_CRITICAL(eth_w5500_init(&eth_callbacks), "Ethernet fail");
  eth_w5500_force_link_blocking(on_eth_boot_wait);
}
