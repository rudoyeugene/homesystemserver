package com.rudyii.hsw.services;

import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.configuration.Logger;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.objects.events.SystemStateChangedEvent;
import com.rudyii.hsw.providers.IPStateProvider;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

import static com.rudyii.hs.common.names.StaticData.BY_SYSTEM;
import static com.rudyii.hs.common.type.SystemModeType.AUTOMATIC;
import static com.rudyii.hs.common.type.SystemModeType.MANUAL;
import static com.rudyii.hs.common.type.SystemStateType.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class ArmedStateService {
    private final EventService eventService;
    private final IPStateProvider ipStateProvider;
    private final Logger logger;
    private final AtomicInteger count = new AtomicInteger();
    private SystemModeType systemMode = AUTOMATIC;
    private SystemStateType systemState = RESOLVING;

    @Scheduled(initialDelayString = "10000", fixedRateString = "60000")
    public void run() {
        if (systemMode.equals(AUTOMATIC)) {
            switch (systemState) {
                case ARMED:
                    if (ipStateProvider.mastersOnline()) {
                        log.info("System {}", systemState);
                        disarm(BY_SYSTEM);
                    }
                    break;
                case DISARMED:
                    if (!ipStateProvider.mastersOnline()) {
                        log.info("System {}", systemState);
                        arm(BY_SYSTEM);
                    }
                    break;
                case RESOLVING:
                    if (ipStateProvider.mastersOnline()) {
                        log.info("System {}", systemState);
                        disarm(BY_SYSTEM);
                    } else {
                        log.info("System {}", systemState);
                        arm(BY_SYSTEM);
                    }

            }
        }
        logger.printAdditionalInfo("System is UP for " + count.incrementAndGet() + " minutes");
    }

    private void disarm(String by) {
        if (!DISARMED.equals(systemState)) {
            this.systemState = DISARMED;
            eventService.publish(SystemStateChangedEvent.builder()
                    .systemMode(getSystemMode())
                    .systemState(DISARMED)
                    .by(by)
                    .build());

            log.info("System {}", DISARMED);
        } else {
            log.info("System already {}", DISARMED);
        }
    }

    private void arm(String by) {
        if (!ARMED.equals(systemState)) {
            this.systemState = ARMED;
            eventService.publish(SystemStateChangedEvent.builder()
                    .systemMode(getSystemMode())
                    .systemState(ARMED)
                    .by(by)
                    .build());

            log.info("System {}", ARMED);
        } else {
            log.info("System already {}", ARMED);
        }
    }

    @Async
    @EventListener(ArmedEvent.class)
    public void onEvent(ArmedEvent event) {
        if (event.getSystemMode().equals(MANUAL) && event.getSystemState().equals(ARMED)) {
            this.systemMode = MANUAL;
            arm(event.getBy() == null ? BY_SYSTEM : event.getBy());
        } else if (event.getSystemMode().equals(MANUAL) && event.getSystemState().equals(DISARMED)) {
            this.systemMode = MANUAL;
            disarm(event.getBy() == null ? BY_SYSTEM : event.getBy());
        } else if (event.getSystemMode().equals(AUTOMATIC)) {
            this.systemMode = AUTOMATIC;
            this.systemState = RESOLVING;
            eventService.publish(SystemStateChangedEvent.builder()
                    .systemMode(getSystemMode())
                    .systemState(RESOLVING)
                    .by(event.getBy() == null ? BY_SYSTEM : event.getBy())
                    .build());

        } else {
            log.info("Unsupported case: mode {} and state {}", event.getSystemMode(), event.getSystemState());
        }
    }

    public void disarmBy(String email) {
        disarm(email);
    }

    public boolean isSystemInAutoMode() {
        return systemMode.equals(AUTOMATIC);
    }

    public boolean isArmed() {
        return ARMED.equals(systemState);
    }

    public SystemModeType getSystemMode() {
        return systemMode;
    }

    public SystemStateType getSystemState() {
        return systemState;
    }
}
