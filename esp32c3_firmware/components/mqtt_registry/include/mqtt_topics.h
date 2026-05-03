#ifndef MQTT_TOPICS_H_
#define MQTT_TOPICS_H_

// from ESP32C3 to RELAY
#define MQTT_TOPIC_DOORBELL_ON_RING     "ids/doorbell/on/ring"
#define MQTT_TOPIC_ENV_STATUS           "ids/env/status"
#define MQTT_TOPIC_ON_DOORBELL_MODE_SET "ids/on/doorbell/mode/set"
#define MQTT_TOPIC_ON_DOORBELL_RING     "ids/on/doorbell/ring"

// from RELAY to ESP32C3
#define MQTT_TOPIC_DOORBELL_MODE_SET    "ids/doorbell/mode/set"
#define MQTT_TOPIC_DOORBELL_RING        "ids/doorbell/ring"

#endif // MQTT_TOPICS_H_
