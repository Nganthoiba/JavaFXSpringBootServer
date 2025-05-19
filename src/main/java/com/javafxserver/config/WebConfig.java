package com.javafxserver.config;

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
		
		//By default retrieve all the cors.allowed-origins from the application.properties file and then addthem to the config
		for(String origin : allowedOrigins) {
			Config.addCorsOrigin(origin);
		}
		corsService.setAllowedOrigins(Config.getCorsOrigins());
		
		if(corsService.getAllowedOrigins().isEmpty()) {
			// If no origins are specified, allow all origins
			registry.addMapping("/**")
				.allowedOrigins("*")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
		} else {
			// If specific origins are specified, apply them
			registry.addMapping("/**")
				.allowedOrigins(corsService.getAllowedOrigins().toArray(new String[0]))
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*");
		}
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
