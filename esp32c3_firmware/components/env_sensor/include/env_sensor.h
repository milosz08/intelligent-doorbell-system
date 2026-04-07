#ifndef ENV_SENSOR_H_
#define ENV_SENSOR_H_

#include "driver/gpio.h"
#include "esp_err.h"

#define ONEWIRE_BUS_GPIO        GPIO_NUM_2  /*!< IO2 pin for temp sensor (DQ), pullup by 4.7k resistor to 3.3V. */

/*! \brief Initializes the 1-Wire bus and the DS18B20 temperature sensor.
 *
 * Configures the RMT peripheral for 1-Wire communication on the defined GPIO and scans the bus for the first available
 * DS18B20 device.
 *
 * \retval ESP_OK               On success.
 * \retval ESP_ERR_NOT_FOUND    If no DS18B20 device is detected on the bus.
 * \retval ESP_FAIL             On generic hardware initialization failure.
 */
esp_err_t env_sensor_init(void);

/*! \brief Triggers a measurement and reads the current temperature.
 *
 * This function triggers a hardware conversion and safely blocks the calling task for ~800ms (via FreeRTOS vTaskDelay)
 * to wait for the 12-bit conversion.
 *
 * \param[out] out_temp_c Pointer to a float to store the temperature in Celsius.
 *
 * \retval ESP_OK                   On success.
 * \retval ESP_ERR_INVALID_STATE    If the sensor was not initialized.
 * \retval ESP_FAIL                 If the reading fails (e.g., CRC error or disconnected bus).
 */
esp_err_t env_sensor_read_temperature(float *out_temp_c);

#endif // ENV_SENSOR_H_
