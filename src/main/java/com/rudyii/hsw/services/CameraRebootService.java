package com.rudyii.hsw.services;

import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static java.util.Arrays.asList;

@Slf4j
@Service
public class CameraRebootService {
    private Camera[] cameras;
    private ArmedStateService armedStateService;
    private ThreadPoolTaskExecutor hswExecutor;

    @Autowired
    public CameraRebootService(Camera[] cameras, ArmedStateService armedStateService,
                               ThreadPoolTaskExecutor hswExecutor) {
        this.cameras = cameras;
        this.armedStateService = armedStateService;
        this.hswExecutor = hswExecutor;
    }

    @Async
    @EventListener(CameraRebootEvent.class)
    public void performRebootBy(CameraRebootEvent event) {
        asList(cameras).forEach(cameraMotionDetectionController -> {
            if (cameraMotionDetectionController.getCameraName().equals(event.getCameraName())
                    && !cameraMotionDetectionController.isRebootInProgress()) {
                log.info("Got reboot event for Camera {}, initializing reboot sequence", cameraMotionDetectionController.getCameraName());
                hswExecutor.execute(() -> {
                    try {
                        cameraMotionDetectionController.disableMotionDetection();
                    } catch (IOException e) {
                        log.error("Failed to disable Motion detector on Camera {}", cameraMotionDetectionController.getCameraName(), e);
                    }
                    log.info("Motion detection disabled on Camera {}", cameraMotionDetectionController.getCameraName());

                    cameraMotionDetectionController.performReboot();
                    log.info("Rebooting Camera {}, will wait for {} milliseconds.", cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getRebootTimeout());

                    try {
                        Thread.sleep(cameraMotionDetectionController.getRebootTimeout());
                        cameraMotionDetectionController.rebootComplete();
                        log.info("Reboot complete on Camera {}", cameraMotionDetectionController.getCameraName());

                        if (armedStateService.isArmed()) {
                            log.info("Enabling motion detection on Camera {}", cameraMotionDetectionController.getCameraName());
                            cameraMotionDetectionController.enableMotionDetection();
                        }
                    } catch (Exception e) {
                        log.error("Oops, something goes wrong during reboot sequence: ", e);
                    }
                });
            }
        });
    }
}
