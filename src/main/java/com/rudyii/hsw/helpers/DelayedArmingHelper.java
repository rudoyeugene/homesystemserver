package com.rudyii.hsw.helpers;

import com.rudyii.hsw.configuration.Logger;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.rudyii.hs.common.type.SystemModeType.MANUAL;
import static com.rudyii.hs.common.type.SystemStateType.ARMED;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelayedArmingHelper {
    private final EventService eventService;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final Logger logger;

    private boolean idle = true;

    @Async
    void armWithDelayInSeconds() throws InterruptedException {
        if (idle) {
            this.idle = false;
            int seconds = globalSettingsService.getGlobalSettings().getDelayedArmTimeoutSec();

            while (seconds != 0) {
                logger.printAdditionalInfo("System will be ARMED in " + seconds + " seconds...");
                Thread.sleep(1000);
                seconds--;
            }

            eventService.publish(ArmedEvent.builder()
                    .systemMode(MANUAL)
                    .systemState(ARMED)
                    .by("delay")
                    .build());

            this.idle = true;
        } else {
            log.info("Delayed ARM already in progress");
        }
    }
}
