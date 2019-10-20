package com.rudyii.hsw.services;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CameraHealthService {
    private EventService eventService;
    private OptionsService optionsService;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;

    @Autowired
    public CameraHealthService(EventService eventService, OptionsService optionsService,
                               CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.eventService = eventService;
        this.optionsService = optionsService;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @Scheduled(initialDelayString = "10000", fixedDelayString = "600000")
    public void run() {
        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            if ((boolean) optionsService.getCameraOptions(cameraMotionDetectionController.getCameraName()).get("healthCheckEnabled")) {
                if (cameraMotionDetectionController.isRecordingInProgress()) {
                    log.warn("ffprobe skipped on camera: " + cameraMotionDetectionController.getCameraName() + ", recording in progress...");
                } else if (cameraMotionDetectionController.isDetectorEnabled()) {
                    log.warn("ffprobe skipped on camera: " + cameraMotionDetectionController.getCameraName() + ", detector enabled...");
                } else {
                    try {
                        imageProbe(cameraMotionDetectionController.getJpegUrl(), cameraMotionDetectionController.getCameraName());
                        ffprobe(cameraMotionDetectionController.getRtspUrl(), cameraMotionDetectionController.getCameraName());
                    } catch (Exception e) {
                        log.error("Camera " + cameraMotionDetectionController.getCameraName() + " probe failed, rebooting...");
                        rebootCamera(cameraMotionDetectionController);
                    }
                }
            } else {
                log.info("Health checking is disabled for camera: " + cameraMotionDetectionController.getCameraName());
            }
        }
    }

    private void ffprobe(String rtspUrl, String cameraName) throws Exception {
        List<String> probeCommand = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffprobe").exists()) {
                probeCommand.add("/usr/bin/ffprobe");
                probeCommand.add("-i");

            } else if (new File("/usr/bin/avprobe").exists()) {
                probeCommand.add("/usr/bin/avprobe");

            } else {
                log.error("/usr/bin/ffprobe or /usr/bin/avprobe not found, please install, ignoring health checking");
                return;
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffprobe.exe").exists()) {
                probeCommand.add("ffprobe");
                probeCommand.add("-i");

            } else {
                log.error("C:/Windows/System32/ffprobe.exe not found, please install, ignoring health checking");
                return;
            }
        } else {
            log.error("Unsupported OS detected, ignoring health checking");
            return;
        }

        probeCommand.add(rtspUrl);

        ProcessBuilder probeBuilder = new ProcessBuilder(probeCommand);
        Process ffprobeProcess = probeBuilder.start();
        ffprobeProcess.waitFor();

        if (ffprobeProcess.exitValue() == 0) {
            log.info("Video stream probe success on camera: " + cameraName);
        } else {
            throw new Exception("Video probe failed!");
        }

    }

    private void imageProbe(String jpegUrl, String cameraName) throws Exception {
        ImageIO.read(new URL(jpegUrl));
        log.info("Image probe success on camera: " + cameraName);
    }

    private void rebootCamera(CameraMotionDetectionController cameraMotionDetectionController) {
        eventService.publish(new CameraRebootEvent(cameraMotionDetectionController.getCameraName()));
    }
}
