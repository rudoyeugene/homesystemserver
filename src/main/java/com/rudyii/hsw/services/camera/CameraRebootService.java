package com.rudyii.hsw.services.camera;

import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.services.ArmedStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class CameraRebootService {
    private final List<Camera> cameras;
    private final ArmedStateService armedStateService;
    private final ThreadPoolTaskExecutor hswExecutor;

    @Autowired
    public CameraRebootService(List<Camera> cameras, ArmedStateService armedStateService,
                               ThreadPoolTaskExecutor hswExecutor) {
        this.cameras = cameras;
        this.armedStateService = armedStateService;
        this.hswExecutor = hswExecutor;
    }

    @Async
    @EventListener(CameraRebootEvent.class)
    public void performRebootBy(CameraRebootEvent event) {
        cameras.forEach(camera -> {
            if (camera.getCameraName().equals(event.getCameraName())
                    && !camera.isRebootInProgress()) {
                log.info("Got reboot event for Camera {}, initializing reboot sequence", camera.getCameraName());
                hswExecutor.submit(() -> {
                    try {
                        camera.disableMotionDetection();
                    } catch (IOException e) {
                        log.error("Failed to disable Motion detector on Camera {}", camera.getCameraName(), e);
                    }
                    log.info("Motion detection disabled on Camera {}", camera.getCameraName());

                    camera.performReboot();
                    log.info("Rebooting Camera {}, will wait for {} milliseconds.", camera.getCameraName(), camera.getRebootTimeout());

                    try {
                        Thread.sleep(camera.getRebootTimeout());
                        camera.rebootComplete();
                        log.info("Reboot complete on Camera {}", camera.getCameraName());

                        if (armedStateService.isArmed()) {
                            log.info("Enabling motion detection on Camera {}", camera.getCameraName());
                            camera.enableMotionDetection();
                        }
                    } catch (Exception e) {
                        log.error("Oops, something goes wrong during reboot sequence: ", e);
                    }
                });
            }
        });
    }
}
