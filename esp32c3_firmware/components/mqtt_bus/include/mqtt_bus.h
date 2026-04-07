#ifndef MQTT_BUS_H_
#define MQTT_BUS_H_

#include <stdbool.h>

#include "esp_err.h"
#include "cJSON.h"

#define MQTT_QUEUE_SIZE 10          /*!< Max messages in the queue waiting to be processed. */
#define MQTT_TOPIC_NAME_MAX 64      /*!< Max length of the MQTT topic string. */
#define MAX_MQTT_SUBSCRIPTIONS 20   /*!< Max number of registered topics in the router. */

/*! \brief Callback for processing an incoming MQTT message on a specific topic.
 *
 * \param args Parsed JSON object containing the payload parameters. Can be NULL if the incoming message payload was
 *             empty or invalid JSON.
 */
typedef esp_err_t (*mqtt_topic_cb_t)(const cJSON *args);

/*! \brief Configuration structure for the MQTT bus. */
typedef struct
{
    const char *broker_uri; /*!< URI of the MQTT broker ("mqtt://192.168.1.100:1883"). */
    const char *auth_salt;  /*!< Secret salt used for Zero-Touch provisioning password generation. */
} mqtt_bus_config_t;

/*! \brief Initializes the MQTT bus.
 *
 * Creates the FreeRTOS queue and task for routing, generates authentication credentials 
 * using the provided salt and Ethernet MAC, and starts the MQTT client.
 *
 * \param config Configuration structure containing broker URI and auth salt.
 *
 * \retval ESP_OK               If success.
 * \retval ESP_ERR_INVALID_ARG  If config, broker_uri, or auth_salt is NULL.
 * \retval ESP_ERR_NO_MEM       If no heap memory available.
 * \retval ESP_FAIL             If MQTT client initialization failure.
 */
esp_err_t mqtt_bus_init(const mqtt_bus_config_t *config);

/*! \brief Registers a callback for a specific MQTT topic.
 *
 * \param topic     Topic string to subscribe to (e.g., "htas/screen/set").
 * \param callback  Function to execute when message arrives.
 *
 * \retval ESP_OK         If success.
 * \retval ESP_ERR_NO_MEM If the subscription registry is full.
 */
esp_err_t mqtt_bus_register_topic(const char *topic, mqtt_topic_cb_t callback);

/*! \brief Publishes a message to a specific MQTT topic.
 *
 * \param topic     Destination topic.
 * \param payload   Data to send (serialized JSON string). Can be NULL for empty triggers.
 * \param qos       Quality of Service (0, 1, or 2).
 * \param retain    1 to retain message on broker, 0 otherwise.
 *
 * \return `true` if queued for publishing, `false` otherwise.
 */
bool mqtt_bus_publish(const char *topic, const char *payload, int qos, int retain);

#endif // MQTT_BUS_H_
