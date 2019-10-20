package com.rudyii.hsw.services;

import com.rudyii.hsw.helpers.ArmingController;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActionsService {

    private ArmingController armingController;
    private ReportingService reportingService;
    private EventService eventService;

    public ActionsService(ArmingController armingController, ReportingService reportingService,
                          EventService eventService) {
        this.armingController = armingController;
        this.reportingService = reportingService;
        this.eventService = eventService;
    }

    public void performAction(String action) {
        switch (action) {
            case "ARM":
                armingController.forceArm();
                break;
            case "DELAYED_ARM":
                armingController.delayedArm();
                break;
            case "DISARM":
                armingController.forceDisarm();
                break;
            case "AUTOMATIC":
                armingController.automatic();
                break;
            case "RESEND_HOURLY":
                reportingService.sendHourlyReport();
                break;
            default:
                log.error("Unsupported action: " + action);
        }
    }

    public void rebootCamera(String cameraName) {
        eventService.publish(new CameraRebootEvent(cameraName));
    }
}
