package com.rudyii.hsw.springcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.homeSystemInitComplete;

@Component
public class InitCompleteListener implements ApplicationListener<ContextRefreshedEvent> {
    private static Logger LOG = LogManager.getLogger(InitCompleteListener.class);

    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        LOG.info("Home System initialization successfully completed, switching into PRODUCTION mode.");
        homeSystemInitComplete();
    }
}
