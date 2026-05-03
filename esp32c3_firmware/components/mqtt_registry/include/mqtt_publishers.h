#ifndef MQTT_PUBLISHERS_H_
#define MQTT_PUBLISHERS_H_

#include <stdbool.h>

/*! \brief Publishes a ring event to the MQTT broker.
 *
 * This function should be called by the hardware layer to notify the Java server that someone is at the door.
 */
void mqtt_publish_doorbell_ring_event(void);

/*! \brief Packages and publishes the current environmental telemetry data to the MQTT broker.
 *
 * This function handles the JSON serialization of the provided sensor data and queues it for transmission.
 *
 * \param temperature The current temperature value in Celsius to be published.
 */
void mqtt_publish_env_status(float temperature);

/*! \brief Acknowledges the doorbell mode change to the MQTT broker.
 *
 * This function is called after the ESP32 successfully updates its internal state (e.g., silent mode) to synchronize
 * the state with the relay server and clients.
 *
 * \param is_silent The new state of the doorbell: true for silent, false for audible.
 */
void mqtt_publish_on_doorbell_mode_set(bool is_silent);

/*! \brief Publishes a callback event after a manual ring request from a client.
 *
 * This function notifies the server that the doorbell has physically triggered the chime in response to a manual
 * "ring" command from the client application.
 */
void mqtt_publish_on_doorbell_client_ring_event(void);

#endif // MQTT_PUBLISHERS_H_
