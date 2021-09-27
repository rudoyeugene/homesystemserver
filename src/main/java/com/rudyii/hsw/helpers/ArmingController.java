package com.rudyii.hsw.helpers;

import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.system.EventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.rudyii.hs.common.names.StaticData.BY_WEB;
import static com.rudyii.hs.common.type.SystemModeType.AUTOMATIC;
import static com.rudyii.hs.common.type.SystemModeType.MANUAL;
import static com.rudyii.hs.common.type.SystemStateType.*;

@Slf4j
@Component
@AllArgsConstructor
public class ArmingController {
    private final EventService eventService;
    private final DelayedArmingHelper armingHelper;
    private final ArmedStateService armedStateService;

    public void forceArm() {
        try {
            setSystemModeTo(MANUAL, ARMED);
        } catch (Exception e) {
            log.error("Error fire action!", e);
        }
    }

    public void delayedArm() {
        try {
            armingHelper.armWithDelayInSeconds();
        } catch (Exception e) {
            log.error("Error fire action!", e);
        }
    }

    public void forceDisarm() {
        try {
            setSystemModeTo(MANUAL, DISARMED);
        } catch (Exception e) {
            log.error("Error fire action!", e);
        }
    }

    public void automatic() {
        try {
            setSystemModeTo(AUTOMATIC, RESOLVING);
        } catch (Exception e) {
            log.error("Error fire action!", e);
        }
    }

    private void setSystemModeTo(SystemModeType armedMode, SystemStateType armedState) {
        if (armedStateService.getSystemMode().equals(armedMode) && getCurrentArmedState().equals(armedState)) {
            log.info("System already is {}:{}", armedMode, armedState);
        } else {
            eventService.publish(ArmedEvent.builder()
                    .systemMode(armedMode)
                    .systemState(armedState)
                    .by(BY_WEB)
                    .build());
        }
    }

    private SystemStateType getCurrentArmedState() {
        if (armedStateService.isSystemInAutoMode()) {
            return RESOLVING;
        } else {
            return armedStateService.isArmed() ? ARMED : DISARMED;
        }
    }
}
