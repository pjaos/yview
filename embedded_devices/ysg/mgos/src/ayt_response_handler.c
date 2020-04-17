#include "mgos.h"
#include "ydev_ayt_handler.h"

#include "ayt_tx_handler.h"

#define IP_BUF_SIZE 16
#define URL_BUF_SIZE 30
#define JSON_BUFFER_SIZE 4096
struct mg_connection *aty_response_con;

static char sta_ip[IP_BUF_SIZE];
static char jbuf[JSON_BUFFER_SIZE];
static char icons_ip_addr[IP_BUF_SIZE];
static char icons_addr[URL_BUF_SIZE];
static char icons_url[URL_BUF_SIZE+6];

/**
 * Send a response to the are you there message from the ICONS gateway.
 **/
char *get_icons_gw_ip_address(void) {
    if( strlen(icons_ip_addr) > 0 ) {
      return icons_ip_addr;
    }
    return NULL;
}

/**
 * @brief Send a response to the are you there message from the ICONS gateway.
 * @param nc An mg_connection instance.
 **/
void send_ayt_response(struct mg_connection *nc) {
  char *unit_name         = (char *)mgos_sys_config_get_ydev_unit_name();
  char *group_name        = (char *)mgos_sys_config_get_ydev_group_name();
  char *product_id        = (char *)mgos_sys_config_get_ydev_product_id();
  char *services  		    = "WEB:80";
  char *os  				      = "MONGOOSE_OS";
  struct mgos_net_ip_info 	ip_info;
  struct  json_out 		out1 = JSON_OUT_BUF(jbuf, JSON_BUFFER_SIZE);
  //Ensure we don't use NULL pointers.
  if( unit_name == NULL ) {
    unit_name="";
  }
  if( group_name == NULL ) {
    group_name="";
  }
  if( product_id == NULL ) {
    product_id="";
  }

  mg_conn_addr_to_str(nc, icons_ip_addr, sizeof(icons_ip_addr), MG_SOCK_STRINGIFY_REMOTE | MG_SOCK_STRINGIFY_IP );
  mg_conn_addr_to_str(nc, icons_addr, sizeof(icons_addr), MG_SOCK_STRINGIFY_REMOTE | MG_SOCK_STRINGIFY_IP | MG_SOCK_STRINGIFY_PORT );
  snprintf(icons_url, sizeof(icons_url), "udp://%s", icons_addr);

  memset(sta_ip, 0, IP_BUF_SIZE);
  //We don't check wifiStatus == MGOS_WIFI_IP_ACQUIRED as we may miss this state during polling.
  if (mgos_net_get_ip_info(MGOS_NET_IF_TYPE_WIFI, MGOS_NET_IF_WIFI_STA, &ip_info)) {
    if( &ip_info.ip ) {
      mgos_net_ip_to_str(&ip_info.ip, sta_ip);
    }
  }

  //This defines the JSON response message sent after reception of an AYT message.
  static char *beacon_resp_str = "{\n\
UNIT_NAME:%Q,\n\
GROUP_NAME:%Q,\n\
PRODUCT_ID:%Q,\n\
IP_ADDRESS:%Q,\n\
SERVICE_LIST:%Q,\n\
OS:%Q\n\
}";

  json_printf(&out1, beacon_resp_str, unit_name,
                                      group_name,
                    product_id,
                    sta_ip,
                    services,
                    os);

  if( aty_response_con == NULL ) {
  aty_response_con = mg_connect(mgos_get_mgr(), icons_url, NULL, NULL);
  aty_response_con->flags |= MG_F_SEND_AND_CLOSE;
  }

  if( aty_response_con != NULL ) {
  mg_send(aty_response_con, jbuf, strlen(jbuf) );
  aty_response_con = NULL;
  }

}
