package com.rudyii.hsw.springcore;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.homeSystemInitComplete;

@Slf4j
@Component
public class InitCompleteListener implements ApplicationListener<ContextRefreshedEvent> {
    public void onApplicationEvent(@NotNull ContextRefreshedEvent contextRefreshedEvent) {
        log.info("Home System initialization successfully completed, switching into PRODUCTION mode.");
        homeSystemInitComplete();
    }
}
