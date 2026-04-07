#ifndef CYCLIC_TASK_H_
#define CYCLIC_TASK_H_

#include "esp_err.h"

/*! \brief Interval at which the cyclic task processes telemetry data (in milliseconds). */
#define CYCLIC_TASK_INTERVAL_MS 5000

/*! \brief Initializes and starts the background cyclic task.
 *
 * This function creates a FreeRTOS task that periodically polls various system states and sensors and publishes the
 * collected telemetry data to the MQTT broker.
 *
 * It must be called after the hardware (env_sensor) and messaging (mqtt_bus) components have been fully initialized in
 * the main application.
 *
 * \retval ESP_OK           On success.
 * \retval ESP_ERR_NO_MEM   If there is insufficient FreeRTOS heap memory to create the task.
 */
esp_err_t cyclic_task_init(void);

#endif // CYCLIC_TASK_H_
