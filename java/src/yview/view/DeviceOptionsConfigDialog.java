package yview.view;

import java.awt.Frame;

import pja.gui.GenericConfigDialog;
import pja.io.SimpleConfig;
import yview.model.Constants;

public class DeviceOptionsConfigDialog extends GenericConfigDialog {

	public DeviceOptionsConfigDialog(Frame frame, String title, boolean modal) {
		super(frame, title, modal, Constants.APP_NAME);
		
	}
}