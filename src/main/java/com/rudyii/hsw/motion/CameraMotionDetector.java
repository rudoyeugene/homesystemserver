package com.rudyii.hsw.motion;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.objects.events.MotionDetectedEvent;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

import static com.rudyii.hsw.configuration.OptionsService.SHOW_MOTION_AREA;

@Slf4j
@Component
@Scope(value = "prototype")
public class CameraMotionDetector {
    private final OptionsService optionsService;
    private final ArmedStateService armedStateService;

    private final EventService eventService;
    private BufferedImage previousImage, currentImage, motionObject;
    private URL sourceUrl;
    private String cameraName;
    private boolean enabled = false;
    private boolean eventFired = false;

    @Lazy
    @Autowired
    public CameraMotionDetector(EventService eventService, OptionsService optionsService,
                                ArmedStateService armedStateService) {
        this.eventService = eventService;
        this.optionsService = optionsService;
        this.armedStateService = armedStateService;
    }

    @Async
    public void start() throws Exception {
        this.enabled = true;
        log.info("Started CameraMotionDetector on Camera {}", cameraName);

        try {
            this.previousImage = ImageIO.read(sourceUrl);
        } catch (Exception e) {
            log.error("Failed to get previous image from Camera: {}", cameraName);
        }
        try {
            this.currentImage = ImageIO.read(sourceUrl);
        } catch (Exception e) {
            log.error("Failed to get current image from camera: {}", cameraName);
        }

        startDetection();
    }

    @Async
    public void stop() {
        this.enabled = false;
        log.info("Stopping CameraMotionDetector on {}", cameraName);
    }

    private void startDetection() throws InterruptedException {
        while (this.enabled && armedStateService.isArmed()) {
            this.previousImage = currentImage;
            try {
                this.motionObject = new BufferedImage(previousImage.getWidth(), previousImage.getHeight(), previousImage.getType());
                this.currentImage = ImageIO.read(sourceUrl);
                detect();
            } catch (Exception e) {
                log.error("Failed to get current image from camera: {}", cameraName);
                fireRebootEvent();
            }
            Thread.sleep(interval());
        }
    }

    private void detect() {
        int previousImageWidth = previousImage.getWidth(null);
        int currentImageWidth = currentImage.getWidth(null);
        int previousImageHeight = previousImage.getHeight(null);
        int currentImageHeight = currentImage.getHeight(null);

        if ((previousImageWidth != currentImageWidth) || (previousImageHeight != currentImageHeight)) {
            log.error("Images dimensions mismatch: previous image size = {}x{} while current image is {}x{} on Camera {}",
                    previousImageWidth, previousImageHeight, currentImageWidth, previousImageHeight, cameraName);
            return;
        }

        int diff = 0;
        boolean prevPixel = false;
        for (int y = 0; y < previousImageHeight; y++) {
            for (int x = 0; x < previousImageWidth; x++) {
                int rgbCurr = currentImage.getRGB(x, y);
                int redCurr = (rgbCurr >> 16) & 0xFF;
                int greenCurr = (rgbCurr >> 8) & 0xFF;
                int blueCurr = rgbCurr & 0xFF;

                int rgbPrev = previousImage.getRGB(x, y);
                int redPrev = (rgbPrev >> 16) & 0xFF;
                int greenPrev = (rgbPrev >> 8) & 0xFF;
                int bluePrev = rgbPrev & 0xFF;

                boolean currPixel = ((Math.abs(redCurr - redPrev)) > noiseLevel())
                        && ((Math.abs(greenCurr - greenPrev)) > noiseLevel())
                        && ((Math.abs(blueCurr - bluePrev)) > noiseLevel());

                if (prevPixel && currPixel) {
                    motionObject.setRGB(x, y, rgbCurr);
                    diff++;
                }

                prevPixel = currPixel;
            }
        }

        int imageSize = previousImageWidth * previousImageHeight;
        int differenceInPercentage = (100 * diff) / imageSize;

        if (showMotionArea()) {
            System.out.println(cameraName + " noise level: " + noiseLevel() + " and motion area size: " + differenceInPercentage + "%");
        }

        if (differenceInPercentage > motionAreaSize()) {
            log.info("Motion detected on Camera {} with motion area size : {}%", cameraName, differenceInPercentage);
            eventService.publish(new MotionDetectedEvent(cameraName, differenceInPercentage, currentImage, motionObject));
        }
    }

    private void fireRebootEvent() {
        if (!eventFired) {
            log.error("Firing reboot event for Camera {}", cameraName);
            this.eventFired = true;
            eventService.publish(new CameraRebootEvent(cameraName));
        }
    }

    private boolean showMotionArea() {
        return (boolean) optionsService.getOption(SHOW_MOTION_AREA);
    }

    private long motionAreaSize() {
        return ((long) optionsService.getCameraOptions(cameraName).get("motionArea"));
    }

    private long interval() {
        return (long) optionsService.getCameraOptions(cameraName).get("interval");
    }

    private long noiseLevel() {
        return (long) optionsService.getCameraOptions(cameraName).get("noiseLevel");
    }


    public CameraMotionDetector on(Camera camera) throws MalformedURLException {
        this.cameraName = camera.getCameraName();
        this.sourceUrl = new URL(camera.getJpegUrl());

        return this;
    }
}
