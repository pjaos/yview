/*
 * dac.c
 *
 *  Created on: 10 Jan 2020
 *      Author: pja
 */

#include <mgos.h>
#include <driver/dac.h>
#include "cal.h"

/**
 * The DAC is used to pull the TCXO that is fitted to the
 * ADF4350 dev board.
 */

/**
 * @brief Initialise the DAC on GPIO25
 * @return void
 */
void init_cal(void) {
    dac_output_enable (DAC_CHANNEL_1);
    set_cal( mgos_sys_config_get_ydev_dac_cal() );
    LOG(LL_INFO, ("Loaded the DAC calibration value (%d)\n", mgos_sys_config_get_ydev_dac_cal() ) );
}

/**
 * @brief Set the DAC value.
 * @param value The value to write to the DAC.
 */
void set_cal(uint8_t value) {
    dac_output_voltage (DAC_CHANNEL_1, value);
}
