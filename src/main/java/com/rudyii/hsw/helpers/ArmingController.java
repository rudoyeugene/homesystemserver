package com.rudyii.hsw.helpers;

import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.events.ArmedEvent;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.EventService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.rudyii.hsw.enums.ArmedModeEnum.AUTOMATIC;
import static com.rudyii.hsw.enums.ArmedModeEnum.MANUAL;
import static com.rudyii.hsw.enums.ArmedStateEnum.*;

/**
 * Created by jack on 22.01.17.
 */
@Component
public class ArmingController {
    private static Logger LOG = LogManager.getLogger(ArmingController.class);

    private EventService eventService;
    private DelayedArmingHelper armingHelper;
    private ArmedStateService armedStateService;

    @Autowired
    public ArmingController(EventService eventService, DelayedArmingHelper armingHelper, ArmedStateService armedStateService) {
        this.eventService = eventService;
        this.armingHelper = armingHelper;
        this.armedStateService = armedStateService;
    }

    public String forceArm() {
        try {
            setSystemModeTo(MANUAL, ARMED);
            return "System ARMED";
        } catch (Exception e) {
            LOG.error("Error fire action!", e);
            return "Operation unsuccessful!";
        }
    }

    public String delayedArm() {
        try {
            armingHelper.armWithDelayInSeconds();
            return "System ARMED";
        } catch (Exception e) {
            LOG.error("Error fire action!", e);
            return "Operation unsuccessful!";
        }
    }

    public String forceDisarm() {
        try {
            setSystemModeTo(MANUAL, DISARMED);
            return "System DISARMED";
        } catch (Exception e) {
            LOG.error("Error fire action!", e);
            return "Operation unsuccessful!";
        }
    }

    public String automatic() {
        try {
            setSystemModeTo(AUTOMATIC, AUTO);
            return "System is in the AUTOMATIC MODE";
        } catch (Exception e) {
            LOG.error("Error fire action!", e);
            return "Operation unsuccessful!";
        }
    }

    private void setSystemModeTo(ArmedModeEnum armedMode, ArmedStateEnum armedState) {
        if (armedStateService.getArmedMode().equals(armedMode) && getCurrentArmedState().equals(armedState)){
            LOG.info("System already is " + armedMode.toString() + " and " + armedState.toString());
        } else {
            eventService.publish(new ArmedEvent(armedMode, armedState));
        }
    }

    private ArmedStateEnum getCurrentArmedState() {
        if (armedStateService.isSystemInAutoMode()){
            return AUTO;
        } else {
            return armedStateService.isArmed() ? ARMED : DISARMED;
        }
    }
}
