#ifndef MQTT_HANDLERS_H_
#define MQTT_HANDLERS_H_

#include "esp_err.h"
#include "cJSON.h"

/*! \brief Callback triggered when Java server sends a command to change doorbell mode.
 *
 * Expected JSON payload (args): {"silent": true} or {"silent": false}
 *
 * \param args Parsed JSON parameters.
 *
 * \retval ESP_OK               On success.
 * \retval ESP_ERR_INVALID_ARG  If parameters are missing or invalid.
 */
esp_err_t mqtt_handler_doorbell_mode_set(const cJSON *args);

#endif // MQTT_HANDLERS_H_
