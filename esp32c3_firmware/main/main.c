#include "app_state.h"
#include "cyclic_task.h"
#include "env_sensor.h"
#include "eth_w5500.h"
#include "mdns_service.h"
#include "mqtt_bus.h"
#include "mqtt_registry.h"

#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_event.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MAIN";

#define CHECK_CRITICAL(x, msg) do { \
    esp_err_t err_rc = (x); \
    if (err_rc != ESP_OK) { \
        ESP_LOGE(TAG, msg); \
    } \
} while(0)

static void link_state_changed(bool on)
{
    // TODO
}

static void packet_received(void)
{
    // TODO
}

static void on_eth_boot_wait(bool linked)
{
    if (!linked) ESP_LOGW(TAG, "eth not linked");
    else ESP_LOGI(TAG, "eth linked");
    link_state_changed(linked);
}

// public api ----------------------------------------------------------------------------------------------------------

void app_main(void)
{
    // init global state
    CHECK_CRITICAL(app_state_init(), "App state init fail");

    // init sensor
    CHECK_CRITICAL(env_sensor_init(), "Env sensor init fail");

    // init esp-idf services
    CHECK_CRITICAL(esp_netif_init(), "Netif init fail");
    CHECK_CRITICAL(esp_event_loop_create_default(), "EventLoop init fail");
    CHECK_CRITICAL(gpio_install_isr_service(0), "GPIO ISR init fail"); // enable interrupts

    // init ethernet
    eth_callbacks_t eth_callbacks = {
        .on_link_state_changed  = link_state_changed,
        .on_packet_received     = packet_received,
    };
    CHECK_CRITICAL(eth_w5500_init(&eth_callbacks), "Ethernet init fail");
    eth_w5500_force_link_blocking(on_eth_boot_wait);

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
        .auth_salt  = "MojaTajnaSol", // TODO: getting from NVM
    };
    CHECK_CRITICAL(mqtt_bus_init(&mqtt_cfg), "MQTT bus init fail");
    CHECK_CRITICAL(mqtt_registry_init(), "MQTT registry init fail");

    // init cyclic tasks
    CHECK_CRITICAL(cyclic_task_init(), "Cyclic tasks init fail");
}
