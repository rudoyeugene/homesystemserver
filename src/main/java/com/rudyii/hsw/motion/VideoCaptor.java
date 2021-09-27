package com.rudyii.hsw.motion;

import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.objects.events.CaptureEvent;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Lazy
@Slf4j
@Component
@Scope(value = "prototype")
@RequiredArgsConstructor
public class VideoCaptor {
    private final EventService eventService;
    private CameraSettings cameraSettings;

    @Value("#{hswProperties['video.container.type']}")
    private String videoContainerType;
    private String cameraName;
    private String rtspUrl;
    private String rtspTransport;
    private File result;
    private BufferedImage image;
    private long eventTimeMillis;

    @Async
    void startCaptureFrom(Camera camera) {
        this.eventTimeMillis = System.currentTimeMillis();
        this.cameraSettings = camera.getCameraSettings();
        this.cameraName = camera.getCameraName();
        this.rtspUrl = camera.getRtspUrl();

        if ("tcp".equalsIgnoreCase(camera.getRtspTransport())) {
            this.rtspTransport = "tcp";
        } else {
            this.rtspTransport = "udp";
        }

        this.result = new File(System.getProperty("java.io.tmpdir") + "/" + eventTimeMillis + "." + videoContainerType);

        System.out.println("A new motion detected: {}" + new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date()));

        try {
            this.image = ImageIO.read(new URL(camera.getJpegUrl()));
            getFfmpegStream();
        } catch (IOException e) {
            log.error("Failed to process output file", e);
        }

        publishCaptureEvent();
    }

    private void publishCaptureEvent() {
        eventService.publish(CaptureEvent.builder()
                .cameraName(cameraName)
                .uploadCandidate(result)
                .image(image)
                .eventId(eventTimeMillis).build());
    }

    private void getFfmpegStream() throws IOException {
        log.info("Starting capture on camera {}...", cameraName);
        List<String> captureCommand = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffmpeg").exists() || new File("/usr/bin/avconv").exists()) {
                log.info("Linux OS detected, using script bin/capture_motion.sh with params:");
                printParametersIntoLog();
                captureCommand.add("bin/capture_motion.sh");

            } else {
                log.error("/usr/bin/ffmpeg or /usr/bin/avconv not found, please install, ignoring video capture");
                return;
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffmpeg.exe").exists()) {
                printParametersIntoLog();
                captureCommand.add("bin/capture_motion.bat");

            } else {
                log.error("C:/Windows/System32/ffmpeg.exe not found, please install, ignoring video capture");
                return;
            }
        } else {
            log.error("Unsupported OS detected, ignoring video capture");
            return;
        }

        captureCommand.add(rtspUrl);
        captureCommand.add(String.valueOf(cameraSettings.getRecordLength()));
        captureCommand.add(result.getCanonicalPath());
        captureCommand.add(cameraName);
        captureCommand.add(rtspTransport);

        ProcessBuilder captureProcess = new ProcessBuilder(captureCommand);
        runProcess(captureProcess);
    }

    private void printParametersIntoLog() throws IOException {
        log.info("#1 as source: {}", rtspUrl);
        log.info("#2 as record interval in seconds: {}", cameraSettings.getRecordLength());
        log.info("#3 as a capture result: {}", result.getCanonicalPath());
        log.info("#4 as a camera name: {}", cameraName);
        log.info("#5 as an rtsp transport: {}", rtspTransport);
    }

    private void runProcess(ProcessBuilder process) {
        try {
            Process runningProcess = process.inheritIO().start();
            runningProcess.waitFor(cameraSettings.getRecordLength() + 1, TimeUnit.SECONDS);
            if (runningProcess.isAlive()) {
                runningProcess.waitFor(cameraSettings.getRecordLength() / 2, TimeUnit.SECONDS);
                runningProcess.destroy();

                if (runningProcess.isAlive()) {
                    runningProcess.destroyForcibly();
                }
            }
        } catch (Exception e) {
            log.error("Video capture failed!", e);
        }
    }
}
