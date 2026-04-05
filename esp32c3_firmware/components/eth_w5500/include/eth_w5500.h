#ifndef EHT_W5500_H_
#define EHT_W5500_H_

#include "esp_err.h"
#include "driver/spi_master.h"
#include "driver/gpio.h"

#define ETH_SPI_HOST        SPI2_HOST     /*!< SPI Host controller instance (VSPI/HSPI). */
#define ETH_SPI_SCLK_GPIO   GPIO_NUM_4    /*!< GPIO pin for SPI clock (SCK). */
#define ETH_SPI_MOSI_GPIO   GPIO_NUM_6    /*!< GPIO pin for master out slave in (MOSI) - sending data to W5500. */
#define ETH_SPI_MISO_GPIO   GPIO_NUM_5    /*!< GPIO pin for master in slave out (MISO) - receiving data from W5500. */
#define ETH_SPI_CS_GPIO     GPIO_NUM_7    /*!< GPIO pin for chip select (CS) - active low. */
#define ETH_SPI_INT_GPIO    GPIO_NUM_1    /*!< GPIO pin for interrupt (INT) - signals incoming packets. */
#define ETH_SPI_CLOCK_MHZ   20            /*!< SPI Clock speed in MHz. */
#define ETH_SPI_RST_GPIO    GPIO_NUM_3    /*!< GPIO pin for hardware reset (RST) of the W5500 chip. */

/*! \brief Initial silent wait time for auto-negotiation before alerting the user. */
#define ETH_INIT_WAIT_FOR_LINK_MS       2500
/*! \brief Interval between consecutive link status checks during blocking wait. */
#define ETH_INIT_LINK_CHECK_INTERVAL_MS 1000

/*! \brief Callbacks for Ethernet events. */
typedef struct
{
  void (*on_link_state_changed)(bool is_up);  /*!< Called when cable is plugged/unplugged. */
  void (*on_packet_received)(void);           /*!< Called on RX activity (useful for blinking LEDs). */
} eth_callbacks_t;

/*! \brief Callback type for boot-time link waiting process.
 *
 * \param state true if link is detected (success), false if still waiting (alert).
 */
typedef void (*eth_link_wait_cb_t)(bool state);

/*! \brief Initializes the W5500 Ethernet module.
 *
 * Sets up SPI bus, MAC, PHY, and attaches the network interface glue. Configures IP from DHCP.
 *
 * \param callbacks Event handlers for link status and packet activity.
 *
 * \retval ESP_OK         If hardware initialized successfully.
 * \retval ESP_ERR_NO_MEM If no heap memory available.
 * \retval ESP_FAIL       On hardware initialization failure.
 */
esp_err_t eth_w5500_init(const eth_callbacks_t *callbacks);

/*! \brief Blocks system execution until a physical Ethernet link is established.
 *
 * First, it waits silently for `ETH_INIT_WAIT_FOR_LINK_MS` to allow auto-negotiation. If no link is found, it enters an
 * alert loop calling `wait_cb` with `false` every `ETH_INIT_LINK_CHECK_INTERVAL_MS` until a cable is connected.
 *
 * \param wait_cb Callback to update UI or indicators during the waiting process.
 */
void eth_w5500_force_link_blocking(eth_link_wait_cb_t wait_cb);

#endif // EHT_W5500_H_
