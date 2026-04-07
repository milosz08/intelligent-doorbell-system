#ifndef APP_STATE_H_
#define APP_STATE_H_

#include "esp_err.h"

#include <stdbool.h>

#define MAX_STATE_CALLBACKS 5 /*!< Maximum state callbacks count. */

/*! \brief Callback type triggered when the doorbell silent mode state changes.
 *
 * \param is_silent The new state of the doorbell silent mode (`true` if silent, `false` if normal).
 */
typedef void (*app_state_doorbell_cb_t)(bool is_silent);

/*! \brief Initializes the global application state manager.
 *
 * Creates the FreeRTOS mutexes required for thread-safe state access and manipulation. This must be called before any
 * state getters, setters, or registry functions are used.
 *
 * \retval ESP_OK           On success.
 * \retval ESP_ERR_NO_MEM   If no heap memory is available for the mutex creation.
 */
esp_err_t app_state_init(void);

/*! \brief Registers a callback function to be invoked on doorbell state change.
 *
 * Multiple components can register their callbacks here to be notified whenever the doorbell mode changes globally.
 *
 * \param cb The callback function to register.
 *
 * \retval ESP_OK           On success.
 * \retval ESP_ERR_NO_MEM   If the callback registry array is full.
 */
esp_err_t app_state_register_doorbell_cb(app_state_doorbell_cb_t cb);

/*! \brief Thread-safely sets the doorbell silent mode and notifies all subscribers.
 *
 * If the provided state differs from the currently stored state, it updates the internal variable and triggers all
 * registered callback functions. This function uses a mutex, so it is safe to call from MQTT tasks, HTTP tasks, or the
 * main loop.
 *
 * \param is_silent `true` to enable silent mode, `false` to enable normal mode.
 */
void app_state_set_doorbell_silent(bool is_silent);

/*! \brief Thread-safely retrieves the current doorbell silent mode state.
 *
 * \return `true` if the doorbell is in silent mode, `false` otherwise.
 */
bool app_state_get_doorbell_silent(void);

#endif // APP_STATE_H_
