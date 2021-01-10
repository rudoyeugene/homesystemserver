package com.rudyii.hsw.springcore;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@Configuration
@ImportResource("classpath:app-context.xml")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class GlobalConfig {
}
