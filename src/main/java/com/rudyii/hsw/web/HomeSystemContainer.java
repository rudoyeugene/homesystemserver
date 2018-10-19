package com.rudyii.hsw.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class HomeSystemContainer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Value("#{hswProperties['server.port']}")
    private String serverPort;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.setPort(Integer.parseInt(serverPort));
        factory.setContextPath("/admin");
    }
}
