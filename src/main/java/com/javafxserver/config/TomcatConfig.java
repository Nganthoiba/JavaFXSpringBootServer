package com.javafxserver.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * To enable HTTP alongside HTTPS, we define this configuration class that adds an additional HTTP connector
 */

@Configuration
public class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {    
	
	
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
    	int httpPort = Config.getHttpPort();
    	int httpsPort= Config.getHttpsPort();
    	
    	factory.setPort(httpsPort); 
    	
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpPort);
        factory.addAdditionalTomcatConnectors(connector);
    }
}
