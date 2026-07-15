#include "mqtt_bus.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "esp_log.h"
#include "esp_mac.h"
#include "psa/crypto.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "freertos/task.h"
#include "mqtt_client.h"

// private api ---------------------------------------------------------------------------------------------------------

static const char *TAG = "MQTT_BUS";

typedef struct
{
    const char *topic;
    mqtt_topic_cb_t cb;
} mqtt_sub_entry_t;

/*! \brief Structure representing a queued MQTT message. */
typedef struct
{
    char topic[MQTT_TOPIC_NAME_MAX];    /*!< Topic name. */
    char *payload;                      /*!< Payload (heap allocated). Nullable for empty messages. */
} mqtt_msg_t;

static QueueHandle_t s_mqtt_queue = NULL;
static esp_mqtt_client_handle_t s_mqtt_client = NULL;

static mqtt_sub_entry_t s_subscriptions[MAX_MQTT_SUBSCRIPTIONS];
static int s_sub_count = 0;

static void generate_mqtt_credentials(const char *salt, char *username_out, char *password_out)
{
    uint8_t mac[6];
    esp_read_mac(mac, ESP_MAC_ETH);
    
    sprintf(username_out, "%02X%02X%02X%02X%02X%02X",
            mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    char salted_input[128];
    snprintf(salted_input, sizeof(salted_input), "%s%s", username_out, salt);

    uint8_t hash[32];
    size_t hash_len;

    psa_status_t status = psa_hash_compute(
        PSA_ALG_SHA_256,
        (const uint8_t *)salted_input,
        strlen(salted_input),
        hash,
        sizeof(hash),
        &hash_len
    );
    if (status != PSA_SUCCESS)
    {
        ESP_LOGE(TAG, "failed to compute SHA256 hash, error: %d", status);
        memset(hash, 0, sizeof(hash));
    }

    for (int i = 0; i < 32; i++) sprintf(&password_out[i * 2], "%02x", hash[i]);
}

static void mqtt_process_task(void *pvParameters)
{
    mqtt_msg_t msg;
    while (1)
    {
        if (xQueueReceive(s_mqtt_queue, &msg, portMAX_DELAY) != pdTRUE) continue;

        cJSON *args = NULL;
        if (msg.payload != NULL && strlen(msg.payload) > 0)
        {
            args = cJSON_Parse(msg.payload);
            if (args == NULL) ESP_LOGW(TAG, "failed to parse JSON payload on topic: %s", msg.topic);
        }
        bool handled = false;
        for (int i = 0; i < s_sub_count; i++)
        {
            if (strcmp(s_subscriptions[i].topic, msg.topic) != 0) continue;

            esp_err_t res = s_subscriptions[i].cb(args);
            if (res != ESP_OK) ESP_LOGE(TAG, "handler for topic %s failed with code %d", msg.topic, res);
            handled = true;
            break;
        }
        if (!handled) ESP_LOGW(TAG, "no handler registered for topic: %s", msg.topic);
        if (args != NULL) cJSON_Delete(args);
        if (msg.payload != NULL) free(msg.payload);
    }
}

static void mqtt_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{
    esp_mqtt_event_handle_t event = event_data;
    switch ((esp_mqtt_event_id_t)event_id)
    {
        case MQTT_EVENT_CONNECTED:
        {
            ESP_LOGI(TAG, "mqtt connected");
            for (int i = 0; i < s_sub_count; i++)
            {
                esp_mqtt_client_subscribe(s_mqtt_client, s_subscriptions[i].topic, 1);
                ESP_LOGI(TAG, "subscribed to: %s", s_subscriptions[i].topic);
            }
            break;
        }
        case MQTT_EVENT_DISCONNECTED:
        {
            ESP_LOGW(TAG, "mqtt disconnected");
            break;
        }
        case MQTT_EVENT_DATA:
        {
            if (s_mqtt_queue == NULL) break;
            mqtt_msg_t msg;

            int topic_len = event->topic_len < (MQTT_TOPIC_NAME_MAX - 1) ? event->topic_len : (MQTT_TOPIC_NAME_MAX - 1);
            strncpy(msg.topic, event->topic, topic_len);
            msg.topic[topic_len] = '\0';

            if (event->data_len > 0)
            {
                msg.payload = malloc(event->data_len + 1);
                if (msg.payload == NULL)
                {
                    ESP_LOGE(TAG, "failed to allocate memory for mqtt payload");
                    break;
                }
                strncpy(msg.payload, event->data, event->data_len);
                msg.payload[event->data_len] = '\0';
            }
            else msg.payload = NULL;

            if (xQueueSend(s_mqtt_queue, &msg, 0) != pdTRUE)
            {
                ESP_LOGE(TAG, "mqtt queue full, dropping message");
                if (msg.payload != NULL) free(msg.payload);
            }
            break;
        }
        case MQTT_EVENT_ERROR:
        {
            ESP_LOGE(TAG, "mqtt error");
            break;
        }
        default:
            break;
    }
}

// public api ----------------------------------------------------------------------------------------------------------

esp_err_t mqtt_bus_init(const mqtt_bus_config_t *config)
{
    if (config == NULL || config->broker_uri == NULL || config->auth_salt == NULL) return ESP_ERR_INVALID_ARG;
    if (s_mqtt_queue != NULL) return ESP_OK;

    s_mqtt_queue = xQueueCreate(MQTT_QUEUE_SIZE, sizeof(mqtt_msg_t));
    if (s_mqtt_queue == NULL) return ESP_ERR_NO_MEM;

    xTaskCreate(mqtt_process_task, "mqtt_process_task", 4096, NULL, 5, NULL);

    char mqtt_user[13], mqtt_pass[65];
    generate_mqtt_credentials(config->auth_salt, mqtt_user, mqtt_pass);
    ESP_LOGI(TAG, "mqtt auth initialized with username: %s", mqtt_user);

    esp_mqtt_client_config_t mqtt_cfg = {
        .broker.address.uri = config->broker_uri,
        .credentials.username = mqtt_user,
        .credentials.authentication.password = mqtt_pass
    };
    s_mqtt_client = esp_mqtt_client_init(&mqtt_cfg);
    if (s_mqtt_client == NULL) return ESP_FAIL;

    esp_mqtt_client_register_event(s_mqtt_client, ESP_EVENT_ANY_ID, mqtt_event_handler, NULL);
    esp_mqtt_client_start(s_mqtt_client);

    ESP_LOGI(TAG, "mqtt bus init");
    return ESP_OK;
}

esp_err_t mqtt_bus_register_topic(const char *topic, mqtt_topic_cb_t callback)
{
    if (s_sub_count >= MAX_MQTT_SUBSCRIPTIONS)
    {
        ESP_LOGE(TAG, "subscription registry full");
        return ESP_ERR_NO_MEM;
    }
    s_subscriptions[s_sub_count].topic = topic;
    s_subscriptions[s_sub_count].cb = callback;
    s_sub_count++;

    ESP_LOGI(TAG, "registered topic: %s", topic);
    return ESP_OK;
}

bool mqtt_bus_publish(const char *topic, const char *payload, int qos, int retain)
{
    if (s_mqtt_client == NULL) return false;
    
    const char *safe_payload = (payload == NULL) ? "" : payload;
    int msg_id = esp_mqtt_client_publish(s_mqtt_client, topic, safe_payload, 0, qos, retain);
    
    return (msg_id >= 0);
}
