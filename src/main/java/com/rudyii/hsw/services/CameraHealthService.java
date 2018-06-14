package com.rudyii.hsw.services;

import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jack on 04.02.17.
 */
@Service
public class CameraHealthService {
    private static Logger LOG = LogManager.getLogger(CameraHealthService.class);
    private EventService eventService;
    private ArrayList<CameraMotionDetectionController> healthList;

    @Autowired
    public CameraHealthService(EventService eventService, CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.eventService = eventService;
        this.healthList = new ArrayList<>();

        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            if (cameraMotionDetectionController.isHealthCheckEnabled()) {
                healthList.add(cameraMotionDetectionController);
                LOG.info("CameraHealthService enabled for camera: " + cameraMotionDetectionController.getCameraName());
            }
        }
    }

    @Scheduled(initialDelayString = "10000", fixedDelayString = "600000")
    public void run() {
        if (healthList.size() > 0) {
            healthList.forEach((cameraMotionDetectionController) -> {
                if (cameraMotionDetectionController.isRecordingInProgress()) {
                    LOG.warn("ffprobe skipped on camera: " + cameraMotionDetectionController.getCameraName() + ", recording in progress...");
                } else if ( cameraMotionDetectionController.isDetectorEnabled()){
                    LOG.warn("ffprobe skipped on camera: " + cameraMotionDetectionController.getCameraName() + ", detector enabled...");
                } else {
                    try {
                        imageProbe(cameraMotionDetectionController.getJpegUrl(), cameraMotionDetectionController.getCameraName());
                        ffprobe(cameraMotionDetectionController.getRtspUrl(), cameraMotionDetectionController.getCameraName());
                    } catch (Exception e) {
                        LOG.error("Camera " + cameraMotionDetectionController.getCameraName() + " probe failed, rebooting...");
                        rebootCamera(cameraMotionDetectionController);
                    }
                }
            });
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
                LOG.error("/usr/bin/ffprobe or /usr/bin/avprobe not found, please install, ignoring health checking");
                return;
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffprobe.exe").exists()) {
                probeCommand.add("ffprobe");
                probeCommand.add("-i");

            } else {
                LOG.error("C:/Windows/System32/ffprobe.exe not found, please install, ignoring health checking");
                return;
            }
        } else {
            LOG.error("Unsupported OS detected, ignoring health checking");
            return;
        }

        probeCommand.add(rtspUrl);

        ProcessBuilder probeBuilder = new ProcessBuilder(probeCommand);
        Process ffprobeProcess = probeBuilder.start();
        ffprobeProcess.waitFor();

        if (ffprobeProcess.exitValue() == 0) {
            LOG.info("Video stream probe success on camera: " + cameraName);
        } else {
            throw new Exception("Video probe failed!");
        }

    }

    private void imageProbe(String jpegUrl, String cameraName) throws Exception {
        ImageIO.read(new URL(jpegUrl));
        LOG.info("Image probe success on camera: " + cameraName);
    }

    private void rebootCamera(CameraMotionDetectionController cameraMotionDetectionController) {
        eventService.publish(new CameraRebootEvent(cameraMotionDetectionController.getCameraName()));
    }
}
