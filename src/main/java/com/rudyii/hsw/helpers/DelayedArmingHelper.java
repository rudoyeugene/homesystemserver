package com.rudyii.hsw.helpers;

import com.rudyii.hsw.events.ArmedEvent;
import com.rudyii.hsw.services.EventService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.rudyii.hsw.enums.ArmedModeEnum.MANUAL;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;

/**
 * Created by jack on 19.04.17.
 */
@Component
public class DelayedArmingHelper {
    private static Logger LOG = LogManager.getLogger(DelayedArmingHelper.class);

    private EventService eventService;

    @Value("${arm.delay.seconds}")
    private int armDelaySeconds;

    private boolean idle;

    @Autowired
    public DelayedArmingHelper(EventService eventService) {
        this.eventService = eventService;
        this.idle = true;
    }

    @Async
    void armWithDelayInSeconds() throws InterruptedException {
        if (idle) {
            this.idle = false;
            int seconds = armDelaySeconds;

            while (seconds != 0) {
                System.out.println("System will be ARMED in " + seconds + " seconds...");
                Thread.sleep(1000);
                seconds--;
            }

            eventService.publish(new ArmedEvent(MANUAL, ARMED));

            this.idle = true;
        } else {
            LOG.info("Delayed ARM already in progress");
        }
    }
}
