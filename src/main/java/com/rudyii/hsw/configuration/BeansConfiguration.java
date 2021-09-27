package com.rudyii.hsw.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

//@Configuration
public class BeansConfiguration {
    @Value("#{hswProperties['server.port']}")
    private String serverPort;

    @Bean
    public String serverPort() {
        return serverPort;
    }
}
