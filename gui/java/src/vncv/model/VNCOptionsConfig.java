package vncv.model;
import pja.io.*;
import mtightvnc.*;
import yview.view.MainFrame;
import vncv.view.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @brief Responsible for holding./saving and loading the VNC connection parameters
 */
public class VNCOptionsConfig extends SimpleConfig implements Cloneable
{
  public String     serverAddress="";
  public int        serverPort=5900;
  public boolean    windowTitleBarEnabled=true;
  public boolean    controlPanelEnabled=true;
  public String     windowResizeStrategy=OptionsPanel.FIT_TO_DESKTOP;
  public int        windowHeight=0;
  public int        windowWidth=0;
  public int        windowPosX=0;
  public int        windowPosY=0;
  public int        windowScalingPercentage=100;
  public String     password="";
  public String     encoding=OptionsPanel.ENCODING_OPTION_VALUES[7];                       //We default to ZRLE as its the fastest
  public String     compressionLevel=OptionsPanel.COMPRESSION_LEVEL_OPTION_VALUES[1];
  public String     imageQuality=OptionsPanel.JPEG_IMAGE_QUALITY_OPTION_VALUES[3];
  public String     cursorShapesUpdate=OptionsPanel.CURSOR_SHAPE_UPDATES_OPTION_VALUES[2];
  public boolean    useCopyRect=true;
  public boolean    restricedColors=false;
  public String     mouseButtons2And3=OptionsPanel.MOUSE_BUTTONS_2_AND_3_OPTION_VALUES[0];
  public boolean    viewOnly=false;
  public boolean    scaleRemoteCursor=false;
  public boolean    shareDesktop=false;
  public String     desktopName="";
  private String    configPath;
  private String    encryptionKey;
  
  /**
   * @brief Constructor
   * 
   * @param configPath The path to the configuration files.
   */
  public VNCOptionsConfig(String configPath) {
	super(VNCV.PROGRAM_NAME);
    this.configPath=configPath;
    //Encrypting yourself, not secure, just obfuscate slightly
    encryptionKey=System.getProperty("user.name")+SimpleConfig.GetTopLevelConfigPath(VNCV.PROGRAM_NAME)+"01234567";
    encryptionKey=encryptionKey.substring(0, 8);
  }
  
  /**
   * @brief Determine if the config is valid.
   * @return true if valid.
   */
  public boolean isValid() {
    if( serverAddress == null || serverAddress.length() == 0 ) {
      return false;
    }
    if( serverPort <= 0 || serverPort > 65535 ) {
      return false;
    }
    //TODO extend to check all strings are valid
    return true;
  }
  
  /**
   * @brief Return a boolean as a Yes/No string
   * @param value The boolean value
   * @return S sting containign either Yes or No.
   */
  private String getYesNo(boolean value) {
    if( value ) {
      return OptionsPanel.YES;
    }
    return OptionsPanel.NO;
  }
  /*
   * @brief Determine the boolean value of a string. 
   * @param value The string value.
   * @return If the String == Yes then true is returned, eflse false is returned.
   */
  private boolean getYesNo(String v) {
    if( v.equals(OptionsPanel.YES) ) {
      return true;
    }
    return false;
  }
  
  /*
   * @brief Return this object as an array of string options.
   */
  public String[] getOptionsArray() {
    Vector <String>optionList = new Vector<String>();
    optionList.add(OptionsPanel.HOST);
    optionList.add(serverAddress);
    optionList.add(OptionsPanel.PORT);
    optionList.add(""+serverPort);
    optionList.add(OptionsPanel.WINDOW_BORDER);
    optionList.add( getYesNo(windowTitleBarEnabled) );
    optionList.add(OptionsPanel.SHOW_CONTROL_PANEL);
    optionList.add( getYesNo(controlPanelEnabled) );
    optionList.add(OptionsPanel.FRAME_RESIZE_STRATEGY);
    optionList.add(windowResizeStrategy);
    optionList.add(OptionsPanel.FRAME_SIZE);
    optionList.add(""+windowWidth+","+windowHeight);
    optionList.add(OptionsPanel.FRAME_LOCATION);
    optionList.add(""+windowPosX+","+windowPosY);
    optionList.add(OptionsPanel.SCALING_FACTOR_OPTION);
    optionList.add(""+windowScalingPercentage);
    optionList.add(OptionsPanel.PASSWORD);
    optionList.add(password);
    optionList.add(OptionsPanel.ENCODING_OPTION);
    optionList.add(encoding);
    optionList.add(OptionsPanel.COMPRESSION_LEVEL_OPTION);
    optionList.add(compressionLevel);
    optionList.add(OptionsPanel.JPEG_IMAGE_QUALITY_OPTION);
    optionList.add(imageQuality);
    optionList.add(OptionsPanel.CURSOR_SHAPE_UPDATES_OPTION);
    optionList.add(cursorShapesUpdate);
    optionList.add(OptionsPanel.USE_COPYRECT_OPTION);
    optionList.add(getYesNo(useCopyRect));
    optionList.add(OptionsPanel.RESTRICTED_COLORS_OPTION);
    optionList.add(getYesNo(restricedColors));
    optionList.add(OptionsPanel.MOUSE_BUTTONS_2_AND_3_OPTION);
    optionList.add(mouseButtons2And3);
    optionList.add(OptionsPanel.VIEW_ONLY_OPTION);
    optionList.add(getYesNo(viewOnly));
    optionList.add(OptionsPanel.SCALE_REMOTE_CURSOR_OPTION);
    optionList.add(getYesNo(scaleRemoteCursor));
    optionList.add(OptionsPanel.SHARE_DESKTOP_OPTION);
    optionList.add(getYesNo(shareDesktop));
    
    //Return as a Sting array
    String args[] = new String[optionList.size()];
    optionList.toArray(args);
    return args;
  }
  
  /**
   * @brief Set the state of this object from an options array.
   * @param optionsArray An array of string options.
   */
  public void setOptionsArray(String optionsArray[] ) {
    int index=0;

    while( index < optionsArray.length ) {
      try {
        String attributeName=optionsArray[index];
        String attributeValue=optionsArray[index+1];
        
        if( attributeName.equals(OptionsPanel.HOST) ) {
          serverAddress=attributeValue;
        }
        
        if( attributeName.equals(OptionsPanel.PORT) ) {
          serverPort=Integer.parseInt(attributeValue);
        }
        
        if( attributeName.equals(OptionsPanel.WINDOW_BORDER) ) {
          windowTitleBarEnabled=getYesNo(attributeValue);
        }
        
        if( attributeName.equals(OptionsPanel.SHOW_CONTROL_PANEL) ) {
          controlPanelEnabled=getYesNo(attributeValue);
        }
        
        if( attributeName.equals(OptionsPanel.FRAME_RESIZE_STRATEGY) ) {
          windowResizeStrategy=attributeValue;
        }
        
        if( attributeName.equals(OptionsPanel.FRAME_SIZE) ) {
          Scanner sc = new Scanner(attributeValue);
          sc.useDelimiter(",");
          if( sc.hasNextInt() ) {
            windowWidth=sc.nextInt();
          }
          if( sc.hasNextInt() ) {
            windowHeight=sc.nextInt();
          }
          sc.close();
        }
        
        if( attributeName.equals(OptionsPanel.FRAME_LOCATION) ) {
          Scanner sc = new Scanner(attributeValue);
          sc.useDelimiter(",");
          if( sc.hasNextInt() ) {
            windowPosX=sc.nextInt();
          }
          if( sc.hasNextInt() ) {
            windowPosY=sc.nextInt();
          }
          sc.close();
        }
        
        if( attributeName.equals(OptionsPanel.SCALING_FACTOR_OPTION) ) {
          windowScalingPercentage=Integer.parseInt(attributeValue);
        }

        if( attributeName.equals(OptionsPanel.PASSWORD) ) {
          password=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.ENCODING_OPTION) ) {
          encoding=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.COMPRESSION_LEVEL_OPTION) ) {
          compressionLevel=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.JPEG_IMAGE_QUALITY_OPTION) ) {
          imageQuality=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.CURSOR_SHAPE_UPDATES_OPTION) ) {
          cursorShapesUpdate=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.USE_COPYRECT_OPTION) ) {
          useCopyRect=getYesNo(attributeValue);
        }

        if( attributeName.equals(OptionsPanel.RESTRICTED_COLORS_OPTION) ) {
          restricedColors=getYesNo(attributeValue);
        }

        if( attributeName.equals(OptionsPanel.MOUSE_BUTTONS_2_AND_3_OPTION) ) {
          mouseButtons2And3=attributeValue;
        }

        if( attributeName.equals(OptionsPanel.VIEW_ONLY_OPTION) ) {
          viewOnly=getYesNo(attributeValue);
        }

        if( attributeName.equals(OptionsPanel.SCALE_REMOTE_CURSOR_OPTION) ) {
          scaleRemoteCursor=getYesNo(attributeValue);
        }

        if( attributeName.equals(OptionsPanel.SHARE_DESKTOP_OPTION) ) {
          shareDesktop=getYesNo(attributeValue);
        }
        
      }
      catch(ArrayIndexOutOfBoundsException e) {
      }
      catch(NumberFormatException e) {
      }
      index=index+2;
    }
  }
  
  /**
   * @brief Create a clone of this VNCOptionsConfig instance.
   * @return The cloned obejct.
   */
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
  
  /**
   * @brief Save this object state using the VNC server address and port in the filename.
   */
  public void saveSP() throws FileNotFoundException, IllegalAccessException, IOException {
    save(getSPConfigFile(serverAddress, ""+serverPort).getAbsolutePath(), false, encryptionKey);
  }
  
  /**
   * @brief Load this object state from a file named using the VNC server address and port
   */
  public void loadSP(String host, String port) throws Exception {
    load(getSPConfigFile(host, port).getAbsolutePath(), false, encryptionKey);
  }
  
  /**
   * @brief Load a config from an encrypted file.  
   */
  public void loadEncrypted(String f) throws Exception {
    load(f, false, encryptionKey);
  }
  
  /**
   * @brief Delete the config file with the server and port in the filename.
   */
  public void deleteSPConfigFile() {
    File configFile = getSPConfigFile(serverAddress, ""+serverPort);
    if( configFile != null && configFile.exists() ) {
        configFile.delete();
    }
  }
  
  /**
   * @brief Get the config file when the server and port are used in the filename.
   * @return
   */
  public File getSPConfigFile(String host, String port) {
    return new File(configPath, VNCOptionsConfig.class.getName()+"_"+host+"_"+port);
  }
  
  /**
   * @brief Get the config file when the server and port are used in the filename.
   */
  public File getSPConfigFile() {
    return new File(configPath, VNCOptionsConfig.class.getName()+"_VNC_"+serverAddress+"_"+serverPort);
  }

  /**
   * @brief Save this object state using the host name and service name in the filename.
   */
  public void saveHS(String hostname, String serviceName) throws FileNotFoundException, IllegalAccessException, IOException {
    if( hostname != null && serviceName != null ) {
      save(getHSConfigFile(hostname, ""+serviceName).getAbsolutePath(), false, encryptionKey);    
    }
  }
  
  /**
   * @brief Load this object state from a file named using the host name and service name in the filename.
   */
  public void loadHS(String hostname, String serviceName) throws Exception {
    if( hostname != null && serviceName != null ) {
      load(getHSConfigFile(hostname, serviceName).getAbsolutePath(), false, encryptionKey);
    }
  }
  
  /**
   * @brief Delete the config file with the  host name and service name in the filename.
   */
  public void deleteHSConfigFile(String hostname, String serviceName) {
    if( hostname != null && serviceName != null ) {
      File configFile = getHSConfigFile(hostname, ""+serviceName);
      if( configFile != null && configFile.exists() ) {
          configFile.delete();
      }
    }
  }
  
  /**
   * @brief Get the config file when the host name and service name are used in the filename.
   * @return The File instance of the host name and service config file.
   */
  public File getHSConfigFile(String hostname, String serviceName) {
    if( hostname != null && serviceName != null ) {
      return new File(configPath, VNCOptionsConfig.class.getName()+"_"+hostname+"_"+serviceName);
    }
    return null;
  }
    
}
