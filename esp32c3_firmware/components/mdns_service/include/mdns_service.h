#ifndef MDNS_SERVICE_H_
#define MDNS_SERVICE_H_

#include <stddef.h>
#include <stdint.h>

#include "esp_err.h"

/*! \brief Configuration structure for mDNS service. */
typedef struct
{
    const char *hostname;       /*!< Hostname for the device. */
    const char *instance_name;  /*!< Friendly instance name. */
} mdns_service_config_t;

/*! \brief Initializes the mDNS service.
 *
 * This function initializes the mDNS responder using provided configuration.
 *
 * \param config Pointer to the configuration structure.
 *
 * \retval ESP_OK               Initialized successfully.
 * \retval ESP_ERR_INVALID_ARG  If config, hostname or instance_name are NULL.
 * \retval ESP_FAIL             If mDNS initialization fails.
 */
esp_err_t mdns_service_init(const mdns_service_config_t *config);

/*! \brief Blocks execution until the MQTT broker is found.
 *
 * Internally runs a loop attempting to resolve the broker, delaying between attempts.
 *
 * \param out_uri           Target buffer for the resolved URI.
 * \param max_len           Size of the URI buffer.
 * \param search_timeout_ms Timeout for a single mDNS query (e.g., 3000 ms).
 * \param retry_delay_ms    Delay between failed attempts (e.g., 5000 ms).
 */
void mdns_service_wait_for_mqtt_broker(char *out_uri, size_t max_len, uint32_t search_timeout_ms,
                                       uint32_t retry_delay_ms);

#endif // MDNS_SERVICE_H_
