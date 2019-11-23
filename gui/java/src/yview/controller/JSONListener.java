package yview.controller;

import org.json.JSONObject;

public interface JSONListener {
	//Called when we receive data from a device
	public void setJSONDevice(JSONObject jsonDevice);
	//Called when the connection to the ICONS drops
	public void iconsShutdown();
}
