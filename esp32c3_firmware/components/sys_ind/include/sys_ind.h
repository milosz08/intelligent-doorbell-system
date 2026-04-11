#ifndef SYS_IND_H_
#define SYS_IND_H_

#include <stdbool.h>

#include "driver/gpio.h"
#include "esp_err.h"

#define ETH_LINK_GREEN_LED_PIN      GPIO_NUM_21 /*!< GPIO pin for Green LED: Ethernet link UP. */
#define ETH_ACTIVITY_YELLOW_LED_PIN GPIO_NUM_20 /*!< GPIO pin for Yellow LED: Ethernet activity. */
#define STATUS_BLUE_LED_PIN         GPIO_NUM_9  /*!< GPIO pin for Blue LED: System status and priority alerts. */

#define LED_HOLD_TIME_MS            50          /*!< How long to hold the activity LED on per packet. */

/*! \brief Sets up the system indicators.
 *
 * Configures GPIO output pins, creates the FreeRTOS timer for network activity, and starts the background task for the
 * priority-based status LED.
 *
 * \retval ESP_OK           On success.
 * \retval ESP_ERR_NO_MEM   If no heap memory available for task/timer.
 * \retval ESP_FAIL         On GPIO initialization failure.
 */
esp_err_t sys_ind_init(void);

/*! \brief Updates the Ethernet link LED (green).
 *
 * \param on `true` to turn on, `false` to turn off.
 */
void sys_ind_led_eth_set_link(bool on);

/*! \brief Triggers the Ethernet activity LED (yellow).
 *
 * Turns the LED on and resets the internal timer. This creates a smooth blinking effect proportional to network traffic
 * without blocking the caller.
 */
void sys_ind_led_eth_packet_activity(void);

/*! \brief Triggers a single, normal status blink (blue).
 *
 * Used to visually signal discrete, standard events (like a doorbell press). Lowest priority – executes only if no
 * error state is active.
 */
void sys_ind_status_blink_normal(void);

/*! \brief Signals a non-critical error state (blue).
 *
 * Blinks the LED 3 times rapidly. Medium priority – overrides a normal blink, but will not interrupt a critical error
 * sequence.
 */
void sys_ind_status_error_triple(void);

/*! \brief Puts the system into a critical error mode (blue).
 *
 * Highest priority. Flashes the LED continuously to alert the user of a fatal system failure. Overrides all other
 * status indications.
 */
void sys_ind_status_critical_start(void);

#endif // SYS_IND_H_
