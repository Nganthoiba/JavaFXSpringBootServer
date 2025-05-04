package com.javafxserver.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.javafxserver.config.Config;


public class LogWriter {
	public static void writeLog(String logMessage) {
		Config.createAppPath();
		String logFilePath = Config.APP_PATH + File.separator + "DigiSignLog.log";
		File logFile = new File(logFilePath);

        try {
            // Create the log file if it doesn't exist
            logFile.createNewFile();
            // Write to the log file
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("[" + java.time.LocalDateTime.now() + "] "+logMessage+".\n");
                // Add more log entries as needed
            }

        } catch (IOException e) {
            System.err.println("An error occurred while writing to the log file."+e.getMessage());
            e.printStackTrace();
        }
	}
}
