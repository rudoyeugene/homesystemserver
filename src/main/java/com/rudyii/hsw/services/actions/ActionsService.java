package com.rudyii.hsw.services.actions;

import com.rudyii.hsw.helpers.ArmingController;
import com.rudyii.hsw.services.notification.ReportingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ActionsService {
    private final ArmingController armingController;
    private final ReportingService reportingService;

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

    public void resetCamera(String cameraName) {
    }
}
