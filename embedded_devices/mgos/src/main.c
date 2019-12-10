#include "mgos.h"
#include "ydev_ayt_handler.h"

#include "ayt_tx_handler.h"
#include "rpc_handler.h"
#include "timers.h"

/**
 * @brief The MGOS program entry point.
 **/
enum mgos_app_init_result mgos_app_init(void) {

  rpc_init();

  add_ayt_response_handler(send_ayt_response);

  init_timers();

  return MGOS_APP_INIT_SUCCESS;
}
