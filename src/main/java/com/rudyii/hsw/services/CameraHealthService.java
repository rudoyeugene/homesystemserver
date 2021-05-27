package com.rudyii.hsw.services;

import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CameraHealthService {
    private final EventService eventService;
    private final ThreadPoolTaskExecutor hswExecutor;
    private final List<Camera> cameras;

    @Autowired
    public CameraHealthService(EventService eventService, ThreadPoolTaskExecutor hswExecutor,
                               List<Camera> cameras) {
        this.eventService = eventService;
        this.hswExecutor = hswExecutor;
        this.cameras = cameras;
    }

    @Scheduled(initialDelayString = "10000", fixedDelayString = "600000")
    public void run() {
        cameras.forEach(camera -> {
            if (camera.isHealthCheckEnabled() && camera.isOnline()) {
                if (camera.isRecordingInProgress() || camera.isDetectorEnabled()) {
                    log.warn("Camera {} is busy, skipping Health Check", camera.getCameraName());
                } else {
                    hswExecutor.submit(() -> {
                        try {
                            imageProbe(camera.getJpegUrl(), camera.getCameraName());
                            ffprobe(camera.getRtspUrl(), camera.getCameraName(), camera.getRtspTransport());
                        } catch (Exception e) {
                            log.error("Camera {} probe failed, rebooting...", camera.getCameraName());
                            rebootCamera(camera);
                        }
                    });
                }
            }

            if (!camera.isHealthCheckEnabled()) {
                log.info("Health Check is disabled for camera {}", camera.getCameraName());
            } else if (!camera.isOnline()) {
                log.info("Camera {} is OFFLINE, skipping Health Check", camera.getCameraName());
            }
        });
    }

    private void ffprobe(String rtspUrl, String cameraName, String rtspTransport) throws Exception {
        List<String> probeCommand = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffprobe").exists()) {
                probeCommand.add("/usr/bin/ffprobe");
                setRtspTransportType(rtspTransport, probeCommand);

            } else if (new File("/usr/bin/avprobe").exists()) {
                probeCommand.add("/usr/bin/avprobe");
                setRtspTransportType(rtspTransport, probeCommand);

            } else {
                log.error("/usr/bin/ffprobe or /usr/bin/avprobe not found, please install, ignoring health checking");
                return;
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffprobe.exe").exists()) {
                probeCommand.add("ffprobe");
                setRtspTransportType(rtspTransport, probeCommand);

            } else {
                log.error("C:/Windows/System32/ffprobe.exe not found, please install, ignoring health checking");
                return;
            }
        } else {
            log.error("Unsupported OS detected, ignoring health checking");
            return;
        }

        probeCommand.add("-i");
        probeCommand.add(rtspUrl);

        ProcessBuilder probeBuilder = new ProcessBuilder(probeCommand);
        Process ffprobeProcess = probeBuilder.start();
        ffprobeProcess.waitFor();

        if (ffprobeProcess.exitValue() == 0) {
            log.info("Video stream probe success on camera {}", cameraName);
        } else {
            throw new Exception("Video probe failed!");
        }

    }

    private void setRtspTransportType(String rtspTransport, List<String> probeCommand) {
        if ("tcp".equalsIgnoreCase(rtspTransport)) {
            probeCommand.add("-rtsp_transport");
            probeCommand.add("tcp");
        }
    }

    private void imageProbe(String jpegUrl, String cameraName) throws Exception {
        ImageIO.read(new URL(jpegUrl));
        log.info("Image probe success on camera {}", cameraName);
    }

    private void rebootCamera(Camera camera) {
        eventService.publish(new CameraRebootEvent(camera.getCameraName()));
    }
}
