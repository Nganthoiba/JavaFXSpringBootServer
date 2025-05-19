package com.javafxserver.config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

//import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

@SpringBootApplication(scanBasePackages = {
		"com.javafxserver", 
		//"com.javafxserver.exceptionhandler", 
		"com.javafxserver.service", 
		"com.javafxserver.web", 
		"com.javafxserver.web.controllers"})

@Configuration
public class AppConfig {	
	
	public static void main(String[] args) {
        SpringApplication.run(AppConfig.class, args);
    }
	
//	@Bean
//	public LayoutDialect layoutDialect() {
//	    return new LayoutDialect();
//	}
    
}
