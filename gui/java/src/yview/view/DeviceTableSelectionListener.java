package yview.view;

import org.json.JSONObject;

import yview.model.Constants;
import yview.model.ServiceCmd;

/**
 * @brief An interface to provide notification of the selection of a device selected by the user in the DeviceTablePanel
 */
public interface DeviceTableSelectionListener
{
  public void setSelectedDevice(JSONObject jsonDevice, ServiceCmd serviceCmd);
}
