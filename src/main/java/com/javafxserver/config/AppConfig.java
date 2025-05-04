package com.javafxserver.config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(scanBasePackages = {"com.javafxserver", "com.javafxserver.service", "com.javafxserver.web", "com.javafxserver.web.controllers"})
@Configuration
public class AppConfig {	
	
	public static void main(String[] args) {
        SpringApplication.run(AppConfig.class, args);
    }
    
}
