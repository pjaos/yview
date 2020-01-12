#include <mgos.h>
#include <ydev_ayt_handler.h>
#include <adf4350.h>

#include "ayt_tx_handler.h"
#include "rpc_handler.h"
#include "timer.h"
#include "cal.h"

/**
 * @brief The MGOS program entry point.
 **/
enum mgos_app_init_result mgos_app_init(void) {

  rpc_init();

  add_ayt_response_handler(send_ayt_response);

  init_timers();

  //Set the RF output freq to the last stored value
  mgos_adf4350_freq( mgos_sys_config_get_ydev_output_mhz() );
  
  //Set the RF level to the last RF level set
  mgos_sys_config_set_ydev_rf_level(mgos_sys_config_get_ydev_rf_level());

  //If the RF output was last set off then turn if off
  if( !mgos_sys_config_get_ydev_rf_on() ) {
    mgos_power_down(true);
  }

  init_cal();

  return MGOS_APP_INIT_SUCCESS;
}
