package yview.controller;

import org.json.JSONObject;

/**
 * @brief This defines the methods required for a JSONListener instance.
 */
public interface JSONListener {
	//Called when we receive data from a device
	public void setJSONDevice(JSONObject jsonDevice);
	//Called when the connection to the ICONS drops
	public void iconsShutdown();
}
