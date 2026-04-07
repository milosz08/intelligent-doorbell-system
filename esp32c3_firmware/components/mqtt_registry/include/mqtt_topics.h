#ifndef MQTT_TOPICS_H_
#define MQTT_TOPICS_H_

#define MQTT_TOPIC_DOORBELL_RING        "ids/doorbell/ring"     /*!< From ESP32C3 to RELAY. */
#define MQTT_TOPIC_DOORBELL_MODE_SET    "ids/doorbell/mode/set" /*!< From RELAY to ESP32C3. */
#define MQTT_TOPIC_ENV_STATUS           "ids/env/status"        /*!< From ESP32C3 to RELAY. */

#endif // MQTT_TOPICS_H_
