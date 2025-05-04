package com.javafxserver.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.javafxserver.config.Config;

@Service
public class CorsService {
	private final List<String> allowedOrigins = new CopyOnWriteArrayList<>();
	
	public CorsService(){
		allowedOrigins.addAll(Config.getCorsOrigins());
	}
	
	public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
	
	public void setAllowedOrigins(List<String> origins) {
        allowedOrigins.clear();
        allowedOrigins.addAll(origins);
        Config.setCorsOrigins(origins); // Save to config.json
    }
}
