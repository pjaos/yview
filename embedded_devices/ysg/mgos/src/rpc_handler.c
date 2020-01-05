#include <mgos.h>
#include <mgos_rpc.h>
#include <mgos_syslog.h>
#include <adf4350.h>

#include "ayt_tx_handler.h"
#include "rpc_handler.h"

/*
 * @brief Callback handler to set the RF
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_set_freq_mhz(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {
        char *argString=NULL;
        LOG(LL_INFO, ("%s()", __FUNCTION__) );

        json_scanf(args.p, args.len, "{arg0:%Q}", &argString);

        if( argString ) {

                LOG(LL_INFO, ("argString = %s\n", argString) );
                float freqMHz = atof(argString);
                mgos_adf4350_freq(freqMHz);
                LOG(LL_INFO, ("SET freq to %f MHz\n", freqMHz) );
                mgos_sys_config_set_ydev_output_mhz((double)freqMHz);
                mgos_sys_config_save(&mgos_sys_config, false, NULL);
                LOG(LL_INFO, ("Stored freq = %f MHz\n", mgos_sys_config_get_ydev_output_mhz() ) );
                mgos_syslog_log_info(__FUNCTION__, "Stored freq = %f MHz\n", mgos_sys_config_get_ydev_output_mhz());

                free(argString);

                mg_rpc_send_responsef(ri, NULL);

        }

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}


/*
 * @brief Callback handler to set the RF
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_set_level_dbm(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {
        char *argString=NULL;
        LOG(LL_INFO, ("%s()", __FUNCTION__) );

        json_scanf(args.p, args.len, "{arg0:%Q}", &argString);

        if( argString ) {

                LOG(LL_INFO, ("argString = %s\n", argString) );
                int levelDbm = atoi(argString);
                mgos_set_output_level(levelDbm);
                LOG(LL_INFO, ("SET level to %d dBm\n", levelDbm) );
                mgos_sys_config_set_ydev_rf_level(levelDbm);
                mgos_sys_config_save(&mgos_sys_config, false, NULL);
                LOG(LL_INFO, ("Stored RF level = %d MHz\n", mgos_sys_config_get_ydev_rf_level() ) );
                mgos_syslog_log_info(__FUNCTION__, "Stored RF level = %d dBm\n", mgos_sys_config_get_ydev_rf_level());

                free(argString);

                mg_rpc_send_responsef(ri, NULL);

        }

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}


/*
 * @brief Callback handler to set the RF output on/off
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_set_rf_on(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {
        char *argString=NULL;
        LOG(LL_INFO, ("%s()", __FUNCTION__) );

        json_scanf(args.p, args.len, "{arg0:%Q}", &argString);

        if( argString ) {

                LOG(LL_INFO, ("argString = %s\n", argString) );
                int output_on = atoi(argString);
                LOG(LL_INFO, ("output = %d\n", output_on) );
                if( output_on ) {
                  mgos_power_down(false);
                  mgos_sys_config_set_ydev_rf_on(true);
                }
                else {
                  mgos_power_down(true);
                  mgos_sys_config_set_ydev_rf_on(false);
                }
                mgos_sys_config_save(&mgos_sys_config, false, NULL);
                LOG(LL_INFO, ("Stored RF output = %d\n", mgos_sys_config_get_ydev_rf_on() ) );
                mgos_syslog_log_info(__FUNCTION__, "Stored RF output = %d\n", mgos_sys_config_get_ydev_rf_on());

                free(argString);

                mg_rpc_send_responsef(ri, NULL);

        }

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}


/*
 * @brief Callback handler to set the device to factory defaults. This will
 *        cause the device to reboot.
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_factorydefault(struct mg_rpc_request_info *ri,
                                         void *cb_arg,
                                         struct mg_rpc_frame_info *fi,
                                         struct mg_str args) {
        LOG(LL_INFO, ("Reset to factory default configuration and reboot.") );
        mgos_config_reset(MGOS_CONFIG_LEVEL_DEFAULTS);
        mg_rpc_send_responsef(ri, NULL);
        LOG(LL_INFO, ("Reboot now.") );
        mgos_system_restart_after(250);

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}


/*
 * @brief Callback handler to set update the syslog state (enabled/disabled).
 *        If enabled syslog data is sent to the ICONS_GW host address.
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_update_syslog(struct mg_rpc_request_info *ri,
                                         void *cb_arg,
                                         struct mg_rpc_frame_info *fi,
                                         struct mg_str args) {
        LOG(LL_INFO, ("mgos_rpc_ydev_update_syslog()") );
        char *icons_gw_ip_addr = get_icons_gw_ip_address();

        //If syslog is enabled and we have the ICONS GW IP address setup syslog
        if( mgos_sys_config_get_ydev_enable_syslog() && icons_gw_ip_addr ) {
          char *hostname = (char *)mgos_sys_config_get_ydev_unit_name();
          reinit_syslog(icons_gw_ip_addr, hostname);
        }
        //disable syslog.
        else {
          reinit_syslog("", "");
        }

        mg_rpc_send_responsef(ri, NULL);

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}

/*
 * @brief Init all the RPC handlers.
 */
void rpc_init(void) {

        struct mg_rpc *con = mgos_rpc_get_global();

        mg_rpc_add_handler(con, "ydev.factorydefault", NULL, mgos_rpc_ydev_factorydefault, NULL);
        mg_rpc_add_handler(con, "ydev.set_freq_mhz", NULL, mgos_rpc_ydev_set_freq_mhz, NULL);
        mg_rpc_add_handler(con, "ydev.set_level_dbm", NULL, mgos_rpc_ydev_set_level_dbm, NULL);
        mg_rpc_add_handler(con, "ydev.rf_on", NULL, mgos_rpc_ydev_set_rf_on, NULL);
        mg_rpc_add_handler(con, "ydev.update_syslog", NULL, mgos_rpc_ydev_update_syslog, NULL);

}
