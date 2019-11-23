package uk.me.pausten.yview.model;

import java.util.Vector;
import org.json.JSONObject;

/**
 * @Brief Responsible for holding a list of devices at a location.
 */
public class Location {
    public String location="UNKNOWN_LOCATION";
    private Vector<JSONObject> deviceList = new Vector<JSONObject>();

    public Location(String location) { this.location = location; }
    public String getLocation() {return location; }

    public void add(JSONObject device) { deviceList.add(device); }
    public void remove(JSONObject device) { deviceList.remove(device); }
    public void removeAllDevices() { deviceList.removeAllElements(); }
    public Vector<JSONObject> getDeviceList() { return deviceList; }
}
