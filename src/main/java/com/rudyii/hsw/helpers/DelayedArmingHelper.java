package com.rudyii.hsw.helpers;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.services.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.rudyii.hsw.configuration.OptionsService.DELAYED_ARM_INTERVAL;
import static com.rudyii.hsw.enums.ArmedModeEnum.MANUAL;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;

@Slf4j
@Component
public class DelayedArmingHelper {
    private final EventService eventService;
    private final OptionsService optionsService;

    private boolean idle;

    @Autowired
    public DelayedArmingHelper(EventService eventService, OptionsService optionsService) {
        this.eventService = eventService;
        this.optionsService = optionsService;
        this.idle = true;
    }

    @Async
    void armWithDelayInSeconds() throws InterruptedException {
        if (idle) {
            this.idle = false;
            Long seconds = (Long) optionsService.getOption(DELAYED_ARM_INTERVAL);

            while (seconds != 0) {
                System.out.println("System will be ARMED in " + seconds + " seconds...");
                Thread.sleep(1000);
                seconds--;
            }

            eventService.publish(new ArmedEvent(MANUAL, ARMED));

            this.idle = true;
        } else {
            log.info("Delayed ARM already in progress");
        }
    }
}
