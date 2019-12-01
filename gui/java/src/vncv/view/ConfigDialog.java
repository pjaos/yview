package vncv.view;

import javax.swing.*;

import pja.gui.*;
import vncv.model.Config;

import java.util.*;

/**
 * Responsible for providing the user with a GUI for manipulation of the Config.
 */
public class ConfigDialog extends GenericOKCancelDialog 
{
  public static final int FIELD_COLUMNS=20;
  RowPane       rowPane;  
  JLabel        lookAndFeelsJLabel;
  JComboBox     lookAndFeelsJCheckBox;
  UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
  JFrame parent;
  Config config;
  String configFilename;
  
  /**
   * Constructor.
   * 
   * @param parent The parent GUI component for this dialog.
   * @title The title string for the dialog.
   * @model The modal status of the dialog.
   */
  public ConfigDialog(JFrame parent, String title, boolean modal, Config config, String configFilename) {
    super(parent, title, modal);
    this.parent=parent;
    this.config=config;
    this.configFilename=configFilename;
    
    rowPane = new RowPane();
    
    lookAndFeelsJLabel = new JLabel("Look And Feel");
    Vector lookAndFeelList = new Vector();
    for (int i = 0; i < lookAndFeels.length; i++)
    {
      lookAndFeelList.addElement(lookAndFeels[i].getName());
    }
    lookAndFeelsJCheckBox = new JComboBox(lookAndFeelList);
    
    rowPane.add(lookAndFeelsJLabel, lookAndFeelsJCheckBox);
    add(rowPane);
  }
  
  /**
   * Override set visible in order to load config as the dialog becomes visible. 
   */
  public void setVisible(boolean visible) {
    //If becoming visible
    if( visible ) {
      setCurrentConfig();
    }
    super.setVisible(visible);
    
    //If becoming invisible and the ok button was selected
    if( !visible && this.getButtonPanel().isOkSelected() ) {
      config.lookAndFeelIndex=lookAndFeelsJCheckBox.getSelectedIndex();
      saveConfig();
      Dialogs.showOKDialog(parent , "Look And Feel", "The "+lookAndFeels[config.lookAndFeelIndex].getName()+" look and feel will be used the next time you restart this program.");
    }
  }
  
  /**
   * Set the current config state in the GUI fields.
   */
  void setCurrentConfig() {
    try {
      lookAndFeelsJCheckBox.setSelectedIndex(config.lookAndFeelIndex);
    }
    catch(Exception e) {
      config.lookAndFeelIndex=0;
      lookAndFeelsJCheckBox.setSelectedIndex(config.lookAndFeelIndex);
    }
  }
  
  /**
   * Save the config
   */
  public void saveConfig() {
    try {
      config.save(configFilename);
    }
    catch(Exception e) {
      //Should not really have a problem saving the config
      e.printStackTrace();
    }    
  }
  
}
