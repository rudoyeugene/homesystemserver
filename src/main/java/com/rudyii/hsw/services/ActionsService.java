package com.rudyii.hsw.services;

import com.rudyii.hsw.helpers.ArmingController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActionsService {

    private final ArmingController armingController;
    private final ReportingService reportingService;

    public ActionsService(ArmingController armingController, ReportingService reportingService) {
        this.armingController = armingController;
        this.reportingService = reportingService;
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
}
