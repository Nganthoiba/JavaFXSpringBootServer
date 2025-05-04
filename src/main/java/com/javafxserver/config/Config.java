package com.javafxserver.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/*
 * This class is used to read the configuration file
 * */
public class Config {
	
	// The key for the server port
	public static final String SERVER_PORT_KEY = "server_port";
	public static final String PKCS11 = "PKCS11";
	public static final String SUN_PKCS11 = "SunPKCS11";
	
	public static final String HOME_PATH = System.getProperty("user.home");
	public static final String APP_PATH = HOME_PATH + File.separator + "DigiSignServerApp";
	
	
	//Storage path
	public static final String STORAGE_PATH = APP_PATH + File.separator + "storage";
	
	//public path
	public static final String PUBLIC_PATH = System.getProperty("user.dir") + File.separator + "public";	
	// The path of the epass configuration file
	public static final String CONFIG_FILE_PATH = APP_PATH + File.separator + "epass_config.cfg";
	
	public static String PIN = null; //This will be set when the user enters the correct PIN
	// The default server port
	public static final int DEFAULT_SERVER_PORT = 8080;
	public static final int PROXY_SERVER_PORT = 8081;
	
	
	public static String get(String key) {
		// This method should read the configuration file and return the value for the given key
		// For now, we will just return the default values
		switch (key) {
			case SERVER_PORT_KEY:
				return String.valueOf(DEFAULT_SERVER_PORT);
			default:
				// If the key is not found, read the configuration file epass_config.cfg
		    	File configFile = new File(CONFIG_FILE_PATH);
		    	
		    	if(configFile.exists()) {
		    		// Read the file and get the value for the key
		    		Map<String, String> config = parseConfigFile(configFile.getAbsolutePath());
		    		if(config.containsKey(key)) {
		    			return config.get(key);
		    		}
		    	} 
		    	// If the file does not exist, return null
				return null;
		}
	}
	
	
	
	public static void set(String key, String value) {
		// This method should write the value to the configuration file for the given key
		// For now, we will just print the key and value
		System.out.println("Setting " + key + " to " + value);		
		File configFile = new File(CONFIG_FILE_PATH);
		
		if(configFile.exists()) {
			// Write the value to the file
			try {
				Map<String, String> config = parseConfigFile(configFile.getAbsolutePath());
				config.put(key, value);
				// Write the updated config back to the file
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, String> entry : config.entrySet()) {
					sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
				}
				Files.writeString(Paths.get(CONFIG_FILE_PATH), sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Configuration file not found.");
		}
	}
	
	public static Map<String, String> parseConfigFile(String configFilePath) {
		// This method should parse the configuration file and return the key-value pairs
		// For now, we will just return null
		
		// Split and parse into key-value pairs
        Map<String, String> config = new HashMap<>();
		try {
			String content = Files.readString(Paths.get(configFilePath));
			String[] lines = content.split("\\R"); // \\R matches any line break
			
			for (String line : lines) {
				if (line.contains("=")) {
					String[] keyValue = line.split("=", 2);
					if (keyValue.length == 2) {
						config.put(keyValue[0].trim(), keyValue[1].trim());
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return config;
	}
	
	public static File getConfigFile() {
		// This method should return the configuration file
		File configFile = new File(CONFIG_FILE_PATH);
		return configFile;
	}
	
	//method to set configuration file
    public static File setConfigFile(String dllFilePath) throws IOException {
    	
    	Config.createAppPath();
    	
		File configFile = new File(APP_PATH, "epass_config.cfg");
		configFile.setWritable(true);
		try (FileWriter writer = new FileWriter(configFile)) {
			//writer.write("name = ePass2003" + System.currentTimeMillis() + "\n");
			writer.write("name = ePass2003" + "\n");
			writer.write("library = " + dllFilePath + "\n");
			writer.write("slotListIndex  = 0\n");
			writer.write("attributes = compatibility\n");
			return configFile;
		} catch (IOException e) {			
			throw e;
		}
	}
    
    //create APP_PATH if not exist
    public static void createAppPath() {
    	File directory = new File(APP_PATH);
    	if (!directory.exists()) {
    	    directory.mkdirs();
    	}
    }
}
