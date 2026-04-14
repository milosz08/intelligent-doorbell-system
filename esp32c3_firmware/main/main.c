#include "app_state.h"
#include "cyclic_task.h"
#include "doorbell_ctrl.h"
#include "env_sensor.h"
#include "eth_w5500.h"
#include "mdns_service.h"
#include "mqtt_bus.h"
#include "mqtt_publishers.h"
#include "mqtt_registry.h"
#include "sys_ind.h"

#include "esp_log.h"
#include "esp_netif.h"
#include "esp_event.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MAIN";

#define CHECK_CRITICAL(x, msg) do { \
    esp_err_t err_rc = (x); \
    if (err_rc != ESP_OK) { \
        sys_ind_status_critical_start(); \
        ESP_LOGE(TAG, msg); \
    } \
} while(0)

static void on_silent_mode_changed(bool is_silent)
{
    doorbell_ctrl_set_silent_mode(is_silent);
}

static void on_doorbell_pressed(void)
{
    sys_ind_status_blink_normal();
    mqtt_publish_doorbell_ring_event();
}

// public api ----------------------------------------------------------------------------------------------------------

void app_main(void)
{
    // enable interupts (before all)
    esp_err_t isr_err = gpio_install_isr_service(0);
    if (isr_err != ESP_OK && isr_err != ESP_ERR_INVALID_STATE) ESP_LOGE(TAG, "GPIO ISR init fail");

    // init system indicators
    if (sys_ind_init() != ESP_OK) ESP_LOGE(TAG, "Sys ind init fail");

    // init global state
    CHECK_CRITICAL(app_state_init(), "App state init fail");

    // init doorbell control
    CHECK_CRITICAL(doorbell_ctrl_init(), "Doorbell ctrl init fail");
    CHECK_CRITICAL(app_state_register_doorbell_cb(on_silent_mode_changed), "Silent mode handler init fail");
    doorbell_ctrl_set_callback(on_doorbell_pressed);

    // init sensor
    CHECK_CRITICAL(env_sensor_init(), "Env sensor init fail");

    // init esp-idf services
    CHECK_CRITICAL(esp_netif_init(), "Netif init fail");
    CHECK_CRITICAL(esp_event_loop_create_default(), "EventLoop init fail");

    // init ethernet
    eth_callbacks_t eth_callbacks = {
        .on_link_state_changed  = sys_ind_led_eth_set_link,
        .on_packet_received     = sys_ind_led_eth_packet_activity,
    };
    CHECK_CRITICAL(eth_w5500_init(&eth_callbacks), "Ethernet init fail");
    eth_w5500_force_link_blocking(sys_ind_led_eth_set_link);

    // init mdns service and wait for IPv4 address
    char broker_uri[64] = {0};
    mdns_service_config_t mdns_cfg = {
        .hostname       = "esp32c3-doorbell",
        .instance_name  = "Intelligent doorbell",
    };
    CHECK_CRITICAL(mdns_service_init(&mdns_cfg), "mDNS init fail");
    mdns_service_wait_for_mqtt_broker(broker_uri, sizeof(broker_uri), 3000, 5000);

    // init mqtt bus and registry
    mqtt_bus_config_t mqtt_cfg = {
        .broker_uri = broker_uri,
        .auth_salt  = "SecretSalt123", // TODO: getting from NVM
    };
    CHECK_CRITICAL(mqtt_bus_init(&mqtt_cfg), "MQTT bus init fail");
    CHECK_CRITICAL(mqtt_registry_init(), "MQTT registry init fail");

    // init cyclic tasks
    CHECK_CRITICAL(cyclic_task_init(), "Cyclic tasks init fail");

    sys_ind_status_blink_normal();
}
