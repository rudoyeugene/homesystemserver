package com.rudyii.hsw.services;

import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;

@Slf4j
@Service
public class CameraRebootService {
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;
    private ArmedStateService armedStateService;
    private ThreadPoolTaskExecutor hswExecutor;

    @Autowired
    public CameraRebootService(CameraMotionDetectionController[] cameraMotionDetectionControllers, ArmedStateService armedStateService,
                               ThreadPoolTaskExecutor hswExecutor) {
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
        this.armedStateService = armedStateService;
        this.hswExecutor = hswExecutor;
    }

    @Async
    @EventListener(CameraRebootEvent.class)
    public void performRebootBy(CameraRebootEvent event) {
        asList(cameraMotionDetectionControllers).forEach(cameraMotionDetectionController -> {
            if (cameraMotionDetectionController.getCameraName().equals(event.getCameraName())
                    && !cameraMotionDetectionController.isRebootInProgress()) {
                log.info("Got reboot event for Camera {}, initializing reboot sequence", cameraMotionDetectionController.getCameraName());
                hswExecutor.execute(() -> {
                    cameraMotionDetectionController.disableMotionDetection();
                    log.info("Motion detection disabled on Camera {}", cameraMotionDetectionController.getCameraName());

                    cameraMotionDetectionController.performReboot();
                    log.info("Rebooting Camera {}, will wait for {} milliseconds.", cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getRebootTimeout());

                    try {
                        Thread.sleep(cameraMotionDetectionController.getRebootTimeout());
                        cameraMotionDetectionController.rebootComplete();
                        log.info("Reboot complete on Camera {}", cameraMotionDetectionController.getCameraName());

                        if (armedStateService.isArmed()) {
                            cameraMotionDetectionController.enableMotionDetection();
                            log.info("Enabling motion detection on Camera {}", cameraMotionDetectionController.getCameraName());
                        }
                    } catch (Exception e) {
                        log.error("Oops, something goes wrong during reboot sequence: ", e);
                    }
                });
            }
        });
    }
}
