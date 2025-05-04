package com.javafxserver.service;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import com.javafxserver.config.AppConfig;

@Service
public class ServerService {
	private boolean serverRunning = false;
    private ConfigurableApplicationContext context;
    
    public boolean isServerRunning() {
    	return this.serverRunning && context!= null;
    }
    
    public void start() throws Exception {
        try {
            context = new SpringApplicationBuilder(AppConfig.class)
                    .web(WebApplicationType.SERVLET)
                    .run();
            serverRunning = true;
            // Wait for Spring Boot context to fully initialize (if required)
            context.addApplicationListener(event -> {
                if (event instanceof ApplicationReadyEvent) {
                    System.out.println("Server is fully initialized!");
                }
            });

        } catch (Exception e) {
            serverRunning = false;
            throw new Exception("Failed to start server: " + e.getMessage(), e);
        }
    }
    
    public void stop() throws Exception{
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
            	throw new Exception("Failed to stop server: " + e.getMessage());
            } finally {
                serverRunning = false;
            }
        }
    }

}
