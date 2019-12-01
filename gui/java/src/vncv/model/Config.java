/*****************************************************************************************
 *                        Copyright 2013 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package vncv.model;

import pja.io.*;
import vncv.view.*;

/**
 * @brief Responsible for holding the GUI config.
 */
public class Config extends SimpleConfig
{
  public int guiWidth=0;
  public int guiHeight=0;
  public int guiX=0;
  public int guiY=0;
  public int lookAndFeelIndex=0;
 
  /**
   * Constructor
   */
  public Config() {
	super(VNCV.PROGRAM_NAME);
    Config.SetDefaultConfig(this);
  }
  
  /**
   * @brief Determine if the config is valid.
   * @return true if valid.
   */
  public boolean isValid() {
	if( guiWidth > 0 && guiHeight > 0 ) {
		return true;
	}
	return false;
  }
  
  /**
   * Reset the config to the default values
   */
  public static void SetDefaultConfig(Config config) {
    config.guiWidth=1024;
    config.guiHeight=768;
    config.guiX=0;
    config.guiY=0;
    config.lookAndFeelIndex=0;
  }
  
}
