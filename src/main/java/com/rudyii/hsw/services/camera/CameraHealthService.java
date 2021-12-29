package com.rudyii.hsw.services.camera;

import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.services.system.EventService;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
            boolean isOnline = camera.isOnline();
            boolean isHealthCheckEnabled = camera.getCameraSettings().isHealthCheckEnabled();
            if (isHealthCheckEnabled && isOnline) {
                if (camera.isRecordingInProgress() || camera.isDetectorEnabled()) {
                    log.warn("Camera {} is busy, skipping Health Check", camera.getCameraName());
                } else {
                    hswExecutor.submit(() -> {
                        try {
                            imageProbe(camera.getJpegUrl(), camera.getCameraName());
                            ffprobe(camera.getRtspUrl(), camera.getCameraName());
                        } catch (Exception e) {
                            log.error("Camera {} probe failed, rebooting...", camera.getCameraName());
                            rebootCamera(camera);
                        }
                    });
                }
            } else if (!isHealthCheckEnabled) {
                log.info("Health Check is disabled for camera {}", camera.getCameraName());
            } else {
                log.info("Camera {} is OFFLINE, skipping Health Check", camera.getCameraName());
            }
        });
    }

    private void ffprobe(String rtspUrl, String cameraName) throws Exception {
        FFprobe ffprobe;
        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffmpeg").exists() && new File("/usr/bin/ffprobe").exists()) {
                ffprobe = new FFprobe("/usr/bin/ffprobe");
            } else {
                throw new IOException("/usr/bin/ffmpeg & /usr/bin/ffprobe are not found, can't capture");
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffmpeg.exe").exists() && new File("C:/Windows/System32/ffprobe.exe").exists()) {
                ffprobe = new FFprobe("C:/Windows/System32/ffprobe.exe");
            } else {
                throw new IOException("C:/Windows/System32/ffmpeg.exe & C:/Windows/System32/ffprobe.exe are not found, can't capture");
            }
        } else {
            log.error("Unsupported OS detected, ignoring video capture");
            return;
        }

        FFmpegProbeResult fFmpegProbeResult = ffprobe.probe(rtspUrl);
        if (fFmpegProbeResult.hasError()) {
            throw new Exception("Video probe failed on " + cameraName);
        } else {
            log.info("Video stream probe success on camera {}", cameraName);
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
