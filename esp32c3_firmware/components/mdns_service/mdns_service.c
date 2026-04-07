#include "mdns_service.h"

#include <stdio.h>
#include <string.h>

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "mdns.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MDNS";

static esp_err_t resolve_mqtt_broker(char *out_uri, size_t max_len, uint32_t timeout_ms)
{
    if (out_uri == NULL || max_len == 0)
    {
        ESP_LOGE(TAG, "invalid arguments");
        return ESP_ERR_INVALID_ARG;
    }
    ESP_LOGD(TAG, "searching for _mqtt._tcp service...");

    mdns_result_t *results = NULL;
    esp_err_t err = mdns_query_ptr("_mqtt", "_tcp", timeout_ms, 1, &results);

    if (err != ESP_OK || results == NULL)
    {
        ESP_LOGW(TAG, "mqtt broker not found via mdns");
        if (results) mdns_query_results_free(results);
        return ESP_ERR_NOT_FOUND;
    }
    esp_err_t ret = ESP_ERR_NOT_FOUND;
    mdns_ip_addr_t *a = results->addr;

    while (a)
    {
        if (a->addr.type == ESP_IPADDR_TYPE_V4)
        {
        snprintf(out_uri, max_len, "mqtt://" IPSTR ":%d", IP2STR(&a->addr.u_addr.ip4), results->port);
        ESP_LOGI(TAG, "resolved broker uri: %s", out_uri);
        ret = ESP_OK;
        break;
        }
        a = a->next;
    }
    mdns_query_results_free(results);
    return ret;
}

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t mdns_service_init(const mdns_service_config_t *config)
{
    if (config == NULL || config->hostname == NULL || config->instance_name == NULL)
    {
        ESP_LOGE(TAG, "invalid arguments");
        return ESP_ERR_INVALID_ARG;
    }
    ESP_LOGI(TAG, "initializing mdns with hostname: %s", config->hostname);

    esp_err_t err = mdns_init();
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "failed to initialize mdns: %s", esp_err_to_name(err));
        return err;
    }
    mdns_hostname_set(config->hostname);
    mdns_instance_name_set(config->instance_name);

    return ESP_OK;
}

void mdns_service_wait_for_mqtt_broker(char *out_uri, size_t max_len, uint32_t search_timeout_ms,
                                       uint32_t retry_delay_ms)
{
    ESP_LOGI(TAG, "waiting for mqtt broker via mdns...");

    while (resolve_mqtt_broker(out_uri, max_len, search_timeout_ms) != ESP_OK)
    {
        ESP_LOGI(TAG, "retrying in %lu ms...", (unsigned long)retry_delay_ms);
        vTaskDelay(pdMS_TO_TICKS(retry_delay_ms));
    }
}
