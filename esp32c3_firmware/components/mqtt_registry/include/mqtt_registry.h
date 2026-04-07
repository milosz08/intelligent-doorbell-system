#ifndef MQTT_REGISTRY_H_
#define MQTT_REGISTRY_H_

#include "esp_err.h"

/*! \brief Initializes the MQTT Registry.
 *
 * Binds all application-specific topics to their respective C handler functions. Must be called AFTER the mqtt_bus is
 * initialized.
 *
 * \retval ESP_OK   On success.
 * \retval ESP_FAIL If registration fails (e.g., registry full).
 */
esp_err_t mqtt_registry_init(void);

#endif // MQTT_REGISTRY_H_
