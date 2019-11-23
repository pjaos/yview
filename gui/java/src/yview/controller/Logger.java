/*****************************************************************************************************
 *                                Copyright 2015 Paul Austen                                         *
 *                                                                                                   *
 * This program is distributed under the terms of the GNU Lesser General Public License (version 3)  *
 *****************************************************************************************************/
package yview.controller;

import java.util.Calendar;

/**
 * Light weight Logger class to replace the previously used org.apache.log4j.Logger
 * which was considered to heavy weight and large for this project.
 * 
 * @author Paul Austen
 */
public class Logger
{
  public static final int LEVEL_INFO = 1;
  public static final int LEVEL_DEBUG = 2;
  public static final int LEVEL_ERROR = 3;
  public static final String INFO_TEXT  =   "INFO:  ";
  public static final String DEBUG_TEXT =   "DEBUG: ";
  public static final String ERROR_TEXT =   "ERROR: ";
  private Class loggerClass;
  private boolean debugEnabled=false;
  private boolean infoEnabled=true;
  private static int LogLevel=LEVEL_INFO;
  
  /**
   * Constructor
   * @param loggerClass
   */
  public Logger(Class loggerClass) {
    this.loggerClass=loggerClass;
  }
  
  /**
   * This method should be used to gain a reference to a logger class rather than using the Constructor.
   * @param loggerClass The class that the logger is to reference.
   * @return
   */
  public static Logger GetLogger(Class loggerClass) {
    return new Logger(loggerClass);
  }
  
  /**
   * Show the log message to the user
   * @param message The message to show
   */
  private void message(int level, String message) {
    if( level >= LogLevel ) {
      
      String logLevelStr = Logger.INFO_TEXT;
      
      if( level == Logger.LEVEL_DEBUG ) {
        
        logLevelStr = Logger.DEBUG_TEXT;
        
      } else if( level == Logger.LEVEL_ERROR ) {
        
        logLevelStr = Logger.DEBUG_TEXT;
        
      }
      System.out.println(logLevelStr+" "+Calendar.getInstance().getTime()+" "+loggerClass.getName()+" "+message);
    }
  }
  
  /**
   * Record an info message
   * @param message The info message string object
   */
  public void info(String message) {
    message(Logger.LEVEL_INFO, message);
  }
  
  /**
   * Record an error message
   * @param message The error message string buffer object
   */
  public void info(StringBuffer message) {
    info(message.toString());
  }
  
  /**
   * Record an error message
   * @param message The error message string object
   */
  public void error(String message) {
    message(Logger.LEVEL_ERROR, message);
  }
  
  /**
   * Record an error message
   * @param message The error message string buffer object
   */
  public void error(StringBuffer message) {
    error(message.toString());
  }
  
  /**
   * Record an error message
   * @param message The error message string object
   * @param Exception An exception associated with the error
   */
  public void error(String message, Exception e) {
    message(Logger.LEVEL_ERROR, message+" : "+e.getLocalizedMessage());
  }
  
  /**
   * Record an error message
   * @param Exception An exception associated with the error
   */
  public void error(Exception e) {
    message(Logger.LEVEL_ERROR, e.getLocalizedMessage());
  }
  
  /**
   * Record an debug message
   * @param message The debug message string object
   */
  public void debug(String message) {
    message(Logger.LEVEL_DEBUG, message);   
  }
  
  /**
   * Record an debug message
   * @param message The debug message string buffer object
   */
  public void debug(StringBuffer message) {
    debug(message.toString());
  }

  /**
   * Call to check if the logger has debug enabled.
   * @return True if debug is enabled.
   */
  public boolean isDebugEnabled() {
    return debugEnabled;
  }
  
  /**
   * Call to check if the logger has info level messages enabled.
   * @return True if info is enabled.
   */
  public boolean isInfoEnabled() {
    return infoEnabled;
  }
  
  /**
   * Set the level for all instances of the logger class to use.
   * @param logLevel
   */
  public static void SetLevel(int _LogLevel) {
    if( _LogLevel == LEVEL_INFO || _LogLevel == LEVEL_DEBUG || _LogLevel == LEVEL_ERROR ) {
      LogLevel=_LogLevel;
    }
    else {
      System.out.println(ERROR_TEXT+_LogLevel+" is an invalid log level");
      System.exit(-1);
    }
  }
}
