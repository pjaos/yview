package yview.controller;

import com.jcraft.jsch.Logger;

public class SSHLogger implements Logger {

	public boolean isEnabled(int level) {
		return true;
	}

	public void log(int level, String message) {
		String logLevel = "unknown";
		if (level == Logger.FATAL) {
			logLevel = "FATAL: ";
		} else if (level == Logger.ERROR) {
			logLevel = "ERROR: ";
		} else if (level == Logger.WARN) {
			logLevel = "WARN:  ";
		} else if (level == Logger.INFO) {
			logLevel = "INFO:  ";
		} else if (level == Logger.DEBUG) {
			logLevel = "DEBUG: ";
		}
		System.out.println(logLevel + message);
	}
}