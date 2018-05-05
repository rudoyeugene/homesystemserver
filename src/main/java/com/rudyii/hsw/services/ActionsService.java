package com.rudyii.hsw.services;

import com.rudyii.hsw.events.CameraRebootEvent;
import com.rudyii.hsw.helpers.ArmingController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ActionsService {
    private static Logger LOG = LogManager.getLogger(ActionsService.class);

    private ArmingController armingController;
    private ReportingService reportingService;
    private EventService eventService;
    private UpnpService upnpService;

    public ActionsService(ArmingController armingController, ReportingService reportingService,
                          EventService eventService, UpnpService upnpService) {
        this.armingController = armingController;
        this.reportingService = reportingService;
        this.eventService = eventService;
        this.upnpService = upnpService;
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
            case "OPEN_PORTS":
                upnpService.openPorts();
                break;
            case "CLOSE_PORTS":
                upnpService.closePorts();
                break;
            default:
                LOG.error("Unsupported action: " + action);
        }
    }

    public void rebootCamera(String cameraName) {
        eventService.publish(new CameraRebootEvent(cameraName));
    }
}
