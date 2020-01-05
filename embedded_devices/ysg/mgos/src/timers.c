#include "mgos.h"
#include "mgos_syslog.h"

#include "timer.h"

#define SYSLOG_MSG_BUF_SIZE 132                   //The maximum size +1 (null character) of syslog messages.
static char syslog_msg_buf[SYSLOG_MSG_BUF_SIZE];  //The buffer to load syslog messages into ready for transmission.

/**
 * @brief Callback to send periodic updates of the memory and file system state.
 **/
static void timer1_cb(void *arg) {

    if( mgos_sys_config_get_ydev_enable_syslog() ) {
      size_t heap_zize          = mgos_get_heap_size();
      size_t free_heap_size     = mgos_get_free_heap_size();
      size_t min_free_heap_size = mgos_get_min_free_heap_size();
      size_t fs_size            = mgos_get_fs_size();
      size_t fs_free_size       = mgos_get_free_fs_size();

      snprintf(syslog_msg_buf, SYSLOG_MSG_BUF_SIZE, "uptime: %.2lf, heap (size/free/min) %d/%d/%d, fs (size/free) %d/%d",  mgos_uptime(), heap_zize, free_heap_size, min_free_heap_size, fs_size, fs_free_size);
      mgos_syslog_log_info(__FUNCTION__, syslog_msg_buf);
    }

    (void) arg;
}


/***
 * @brief Init all timer functions.
 */
void init_timers(void) {

    //Setup timer callbacks
    mgos_set_timer(TIMER1_PERIOD_MS, MGOS_TIMER_REPEAT, timer1_cb, NULL);

}
