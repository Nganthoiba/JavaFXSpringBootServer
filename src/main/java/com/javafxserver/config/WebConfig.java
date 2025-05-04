package com.javafxserver.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.javafxserver.service.CorsService;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Value("${cors.allowed-origins}")
    private String[] allowedOrigins; // Spring will automatically split comma-separated values
	
	@Autowired
	private CorsService corsService;
	
	@Override
    public void addCorsMappings(CorsRegistry registry) {
		//corsService.setAllowedOrigins(Arrays.asList(allowedOrigins));
		corsService.setAllowedOrigins(Config.getCorsOrigins());
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins(corsService.getAllowedOrigins().toArray(new String[0])) 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
	
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer
        .favorParameter(false)
        .ignoreAcceptHeader(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("json", MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML);
	}
}
