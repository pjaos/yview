#include "mgos.h"
#include "mgos_rpc.h"
#include "mgos_syslog.h"

#include "ayt_tx_handler.h"
#include "rpc_handler.h"

#define CLOCKWISE_MOTOR_ACTION 1
#define ANTICLOCKWISE_MOTOR_ACTION 2
#define STOP_MOTOR_ACTION 3
#define MAX_MOTOR_STEP_HZ  10

#define STEP_SIZE_1 1
#define STEP_SIZE_1_2 (1/2)
#define STEP_SIZE_1_4 (1/4)
#define STEP_SIZE_1_8 (1/8)
#define STEP_SIZE_1_16 (1/16)
#define STEP_SIZE_1_32 (1/32)

static mgos_timer_id motor_tick_timer = -1;
static uint32_t motorTick;

static void cal(void) {
    //Remove reset
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_rst_slp(), true);
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_step(), true);
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_dir(), true);
}

/**
 * @brief Setup the stepper motor pins.
 */
static void initDRV8825() {

    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_enable(), false);

    //Default to full step
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), false);
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), false);
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), false);

    //Assert reset
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_rst_slp(), false);

    //Step pulses high to step the move the motor, so start low
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_step(), false);

    //Start with motor set to move clockwise
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_dir(), true);

    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_enable(),    MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_m0(),        MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_m1(),        MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_m2(),        MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_rst_slp(),   MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_step(),      MGOS_GPIO_MODE_OUTPUT);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_dir(),       MGOS_GPIO_MODE_OUTPUT);

    mgos_gpio_set_pull(mgos_sys_config_get_drv8825_fault(), MGOS_GPIO_PULL_UP);
    mgos_gpio_set_mode(mgos_sys_config_get_drv8825_fault(),     MGOS_GPIO_MODE_INPUT);

    mgos_msleep(10);
    //Remove reset
    mgos_gpio_setup_output(mgos_sys_config_get_drv8825_rst_slp(), true);
    cal();
}


/***
 * @brief Set the step size of the motor
 */
static void setDRV8825StepSize(float stepSize) {

    if( stepSize >= STEP_SIZE_1 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), false);
    }
    else if( stepSize >= STEP_SIZE_1_2 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), false);
    }
    else if( stepSize >= STEP_SIZE_1_4 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), false);
    }
    else if( stepSize >= STEP_SIZE_1_8 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), false);
    }
    else if( stepSize >= STEP_SIZE_1_16 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), true);
    }
    else if( stepSize >= STEP_SIZE_1_32 ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m0(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m1(), false);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_m2(), true);
    }
}

static void setDRV8825Rotation(int rotation ) {
    if( rotation == CLOCKWISE_MOTOR_ACTION ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_dir(), true);
    }
    else if( rotation == ANTICLOCKWISE_MOTOR_ACTION ) {
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_dir(), false);
    }
}

static void motor_tick_timer_cb(void *arg) {
/*
    if speed == 0.5 then the clock rate would be half the max
                0.1 the 10% of max
    if motorTick%
    */
    if( motor_tick_timer != -1 ) {
        mgos_gpio_toggle(mgos_sys_config_get_drv8825_step());
/*
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_step(), true);
        mgos_gpio_setup_output(mgos_sys_config_get_drv8825_step(), false);
*/
    }

    motorTick++;
}

static void mon_cb(void *arg) {

    LOG(LL_INFO, ("PJA: motorTick=%d",motorTick) );
    (void) arg;
}

/**
 * @brief set the motor state.
 * @param p The pointer to the memory containing json arguments.
 * @param len The length of the above memory.
 * @return void
 */
static void set_motor(const char *p, size_t len, int motor_action) {
    char *stepSizeSelect=NULL;
    char *speedRange=NULL;
    float stepSize=0.0;
    float speedFactor=0.0;
    float stepHz=0.0;
    uint32_t tickNs=0;

    LOG(LL_INFO, ("PJA: motor_action        = %d\n", motor_action) );

    if( motor_action == STOP_MOTOR_ACTION ) {
        if( motor_tick_timer != -1 ) {
            mgos_clear_timer(motor_tick_timer);
            motor_tick_timer=-1;
        }
    }
    else {
        if( motor_tick_timer != -1 ) {
            mgos_clear_timer(motor_tick_timer);
            motor_tick_timer=-1;
        }

        json_scanf(p, len, "{stepSizeSelect:%Q,speedRange:%Q}", &stepSizeSelect, &speedRange);

        if( stepSizeSelect && speedRange ) {
            stepSize = atof(stepSizeSelect);
            speedFactor = 100.0/atof(speedRange);
            setDRV8825StepSize(stepSize);
            stepHz = MAX_MOTOR_STEP_HZ*speedFactor;
            tickNs = 1000000/stepHz;

            LOG(LL_INFO, ("PJA: stepSize    = %.1f\n", stepSize) );
            LOG(LL_INFO, ("PJA: speedFactor = %.1f\n", speedFactor) );
            LOG(LL_INFO, ("PJA: stepHz      = %.1f\n", stepHz) );
            LOG(LL_INFO, ("PJA: tickNs      = %d\n", tickNs) );

            setDRV8825Rotation(motor_action);

            motor_tick_timer = mgos_set_hw_timer(tickNs, MGOS_TIMER_REPEAT, motor_tick_timer_cb, NULL);
        }

        if( stepSizeSelect ) {
            free(stepSizeSelect);
            stepSizeSelect=NULL;
        }

        if( speedRange ) {
            free(speedRange);
            speedRange=NULL;
        }
    }
}

/*
 * @brief Callback handler to be modified as required for the required application.
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_action0(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {

        LOG(LL_INFO, ("PJA: mgos_rpc_ydev_action0()") );

        set_motor(args.p, args.len, CLOCKWISE_MOTOR_ACTION);
        mg_rpc_send_responsef(ri, NULL);

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}

/*
 * @brief Callback handler to be modified as required for the required application.
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_action1(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {
        LOG(LL_INFO, ("PJA: mgos_rpc_ydev_action1()") );

        set_motor(args.p, args.len, ANTICLOCKWISE_MOTOR_ACTION);
        mg_rpc_send_responsef(ri, NULL);

        (void) ri;
        (void) cb_arg;
        (void) fi;
        (void) args;
}

/*
 * @brief Callback handler to stop the motor moving.
 * @param ri
 * @param cb_arg
 * @param fi
 * @param args
 */
static void mgos_rpc_ydev_action2(struct mg_rpc_request_info *ri,
                                  void *cb_arg,
                                  struct mg_rpc_frame_info *fi,
                                  struct mg_str args) {

        LOG(LL_INFO, ("PJA: mgos_rpc_ydev_action2()") );

        set_motor(args.p, args.len, STOP_MOTOR_ACTION);
        mg_rpc_send_responsef(ri, NULL);

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

        initDRV8825();

        mg_rpc_add_handler(con, "ydev.factorydefault", NULL, mgos_rpc_ydev_factorydefault, NULL);
        mg_rpc_add_handler(con, "ydev.action0", NULL, mgos_rpc_ydev_action0, NULL);
        mg_rpc_add_handler(con, "ydev.action1", NULL, mgos_rpc_ydev_action1, NULL);
        mg_rpc_add_handler(con, "ydev.action2", NULL, mgos_rpc_ydev_action2, NULL);
        mg_rpc_add_handler(con, "ydev.update_syslog", NULL, mgos_rpc_ydev_update_syslog, NULL);

        mgos_set_timer(1000, MGOS_TIMER_REPEAT, mon_cb, NULL);
}
