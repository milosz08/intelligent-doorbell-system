#ifndef DOORBELL_CTRL_H_
#define DOORBELL_CTRL_H_

#include <stdbool.h>

#include "esp_err.h"
#include "driver/gpio.h"

#define DOORBELL_SSR_PIN            GPIO_NUM_0  /*!< GPIO pin for the SSR (triggers the physical chime). */
#define DOORBELL_SILENT_RELAY_PIN   GPIO_NUM_10 /*!< GPIO pin for the mechanical relay (disconnects AC for silent mode). */
#define DOORBELL_AC_DETECT_PIN      GPIO_NUM_8 /*!< GPIO pin for the optocoupler (detects 230V AC physical button press). */

#define DOORBELL_SSR_PULSE_MS       500 /*!< Duration in milliseconds to keep the SSR closed when triggered. */
#define DOORBELL_RELAY_ON           0   /*!< Logic level to energize the relay (active low). */
#define DOORBELL_RELAY_OFF          1   /*!< Logic level to de-energize the relay. */

/*! \brief Callback type triggered when a physical doorbell press (230V AC) is detected. */
typedef void (*doorbell_pressed_cb_t)(void);

/*! \brief Initializes the doorbell hardware control module.
 *
 * Configures GPIO pins for the relays and the AC detection optocoupler. Starts the FreeRTOS timer for the SSR pulse and
 * the polling task for AC detection.
 *
 * \retval ESP_OK           On success.
 * \retval ESP_ERR_NO_MEM   If no heap memory is available for the timer or task.
 * \retval ESP_FAIL         On GPIO configuration failure.
 */
esp_err_t doorbell_ctrl_init(void);

/*! \brief Registers a callback function to be invoked on physical button press.
 *
 * \param cb The callback function to register.
 */
void doorbell_ctrl_set_callback(doorbell_pressed_cb_t cb);

/*! \brief Programmatically triggers the doorbell chime.
 *
 * Closes the Solid State Relay (SSR) for a predefined duration (`DOORBELL_SSR_PULSE_MS`) and then automatically opens
 * it using a hardware timer.
 */
void doorbell_ctrl_trigger_chime(void);

/*! \brief Enables or disables the doorbell's hardware silent mode.
 *
 * Controls the mechanical relay to physically disconnect the chime circuit.
 *
 * \param silent `true` to energize the relay and break the NC circuit (silent mode on), `false` to restore normal
 *               operation.
 */
void doorbell_ctrl_set_silent_mode(bool silent);

#endif // DOORBELL_CTRL_H_
