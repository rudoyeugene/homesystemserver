package com.rudyii.hsw.motion;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.objects.events.CaptureEvent;
import com.rudyii.hsw.services.EventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import static com.rudyii.hsw.configuration.OptionsService.RECORD_INTERVAL;

@Slf4j
@Component
@Scope(value = "prototype")
public class VideoCaptor {
    private final EventService eventService;
    private final OptionsService optionsService;

    @Value("#{hswProperties['video.container.type']}")
    private String videoContainerType;
    private String cameraName;
    private String rtspUrl;
    private File result;
    private BufferedImage image;
    private long eventTimeMillis;

    @Autowired
    public VideoCaptor(EventService eventService, OptionsService optionsService) {
        this.eventService = eventService;
        this.optionsService = optionsService;
    }

    @Async
    void startCaptureFrom(Camera camera) {
        this.eventTimeMillis = System.currentTimeMillis();

        this.cameraName = camera.getCameraName();
        this.rtspUrl = camera.getRtspUrl();
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
        captureCommand.add(String.valueOf(optionsService.getOption(RECORD_INTERVAL)));
        captureCommand.add(result.getCanonicalPath());
        captureCommand.add(cameraName);

        ProcessBuilder captureProcess = new ProcessBuilder(captureCommand);
        runProcess(captureProcess);
    }

    private void printParametersIntoLog() throws IOException {
        log.info("#1 as source: {}", rtspUrl);
        log.info("#2 as record interval in seconds: {}", optionsService.getOption(RECORD_INTERVAL));
        log.info("#3 as a capture result: {}", result.getCanonicalPath());
        log.info("#4 as a camera name: {}", cameraName);
    }

    private void runProcess(ProcessBuilder process) {
        try {
            Process runningProcess = process.inheritIO().start();
            runningProcess.waitFor((Long) optionsService.getOption(RECORD_INTERVAL) + 1, TimeUnit.SECONDS);
            if (runningProcess.isAlive()) {
                runningProcess.waitFor((Long) optionsService.getOption(RECORD_INTERVAL) / 2, TimeUnit.SECONDS);
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
