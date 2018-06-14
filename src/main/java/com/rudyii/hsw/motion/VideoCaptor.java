package com.rudyii.hsw.motion;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.objects.Camera;
import com.rudyii.hsw.objects.events.CaptureEvent;
import com.rudyii.hsw.services.EventService;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/**
 * Created by jack on 31.01.17.
 */
@Component
@Scope(value = "prototype")
public class VideoCaptor {
    private static Logger LOG = LogManager.getLogger(VideoCaptor.class);

    private EventService eventService;
    private OptionsService optionsService;

    @Value("#{hswProperties['video.archive.location']}")
    private String archiveLocation;

    private String timeStamp;
    private Camera camera;
    private File result;
    private BufferedImage image;

    @Autowired
    public VideoCaptor(EventService eventService, OptionsService optionsService) {
        this.eventService = eventService;
        this.optionsService = optionsService;
    }

    @Async
    void startCaptureFrom(Camera camera) {
        this.camera = camera;
        generateTimestamps();

        this.result = new File(archiveLocation + "/" + camera.getName() + "_" + timeStamp + ".mp4");

        System.out.println("A new motion detected: " + new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date()));

        try {
            this.image = ImageIO.read(new URL(camera.getJpegUrl()));
            getFfmpegStream();
        } catch (IOException e) {
            LOG.error("Failed to proess output file", e);
        }

        publishCaptureEvent();
    }

    private void publishCaptureEvent() {
        //TODO addbufferedImage to publish with motion record
        eventService.publish(new CaptureEvent(result, image));
    }

    private void generateTimestamps() {
        timeStamp = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date());
    }

    private void getFfmpegStream() throws IOException {
        LOG.info("Starting capture on camera: " + camera.getName() + " ...");
        List<String> captureCommand = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffmpeg").exists() || new File("/usr/bin/avconv").exists()) {
                LOG.info("Linux OS detected, using script bin/capture_motion.sh with params:");
                printParametersIntoLog();
                captureCommand.add("bin/capture_motion.sh");

            } else {
                LOG.error("/usr/bin/ffmpeg or /usr/bin/avconv not found, please install, ignoring video capture");
                return;
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffmpeg.exe").exists()) {
                printParametersIntoLog();
                captureCommand.add("bin/capture_motion.bat");

            } else {
                LOG.error("C:/Windows/System32/ffmpeg.exe not found, please install, ignoring video capture");
                return;
            }
        } else {
            LOG.error("Unsupported OS detected, ignoring video capture");
            return;
        }

        captureCommand.add(camera.getRtspUrl());
        captureCommand.add(String.valueOf(optionsService.getOption(RECORD_INTERVAL)));
        captureCommand.add(result.getCanonicalPath());
        captureCommand.add(camera.getName());

        ProcessBuilder captureProcess = new ProcessBuilder(captureCommand);
        runProcess(captureProcess);
    }

    private void printParametersIntoLog() throws IOException {
        LOG.info("#1 as source: " + camera.getRtspUrl());
        LOG.info("#2 as record interval in seconds: " + String.valueOf(optionsService.getOption(RECORD_INTERVAL)));
        LOG.info("#3 as a capture result: " + result.getCanonicalPath());
        LOG.info("#4 as a camera name: " + camera.getName());
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
            LOG.error("Video capture failed!", e);
        }
    }
}
