package com.rudyii.hsw.springcore;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Created by jack on 28.06.17.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();

        http.authorizeRequests().anyRequest().permitAll();

        http.formLogin().permitAll(false);

        http.logout().permitAll(false);

        http.formLogin().loginPage("/error");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder authManagerBuilder) {
    }
}