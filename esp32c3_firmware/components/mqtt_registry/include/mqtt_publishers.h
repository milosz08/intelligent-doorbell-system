#ifndef MQTT_PUBLISHERS_H_
#define MQTT_PUBLISHERS_H_

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

#endif // MQTT_PUBLISHERS_H_
