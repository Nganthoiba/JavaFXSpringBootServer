package com.javafxserver.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
	private Properties properties;
	public PropertyReader() {
		properties = new Properties();
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
			if(inputStream != null) {
				properties.load(inputStream);
			}
			else {
				throw new RuntimeException("application.properties not found");
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
}
