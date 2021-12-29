package com.rudyii.hsw.motion;

import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.configuration.Logger;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.objects.events.MotionDetectedEvent;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

@Lazy
@Slf4j
@Component
@Scope(value = "prototype")
@RequiredArgsConstructor
public class CameraMotionDetector {
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final EventService eventService;
    private final Logger logger;

    private BufferedImage previousImage, currentImage, motionObject;
    private URL sourceUrl;
    private String cameraName;
    private boolean enabled = false;
    private boolean eventFired = false;
    private CameraSettings cameraSettings;

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
        while (this.enabled) {
            this.previousImage = currentImage;
            try {
                this.motionObject = new BufferedImage(previousImage.getWidth(), previousImage.getHeight(), previousImage.getType());
                this.currentImage = ImageIO.read(sourceUrl);
                detect();
            } catch (Exception e) {
                log.error("Failed to get current image from camera: {}", cameraName);
                fireRebootEvent();
            }
            Thread.sleep(cameraSettings.getIntervalMs());
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

                boolean currPixel = ((Math.abs(redCurr - redPrev)) > cameraSettings.getNoiseLevel())
                        && ((Math.abs(greenCurr - greenPrev)) > cameraSettings.getNoiseLevel())
                        && ((Math.abs(blueCurr - bluePrev)) > cameraSettings.getNoiseLevel());

                if (prevPixel && currPixel) {
                    motionObject.setRGB(x, y, rgbCurr);
                    diff++;
                }

                prevPixel = currPixel;
            }
        }

        int imageSize = previousImageWidth * previousImageHeight;
        int differenceInPercentage = (100 * diff) / imageSize;

        if (globalSettingsService.getGlobalSettings().isShowMotionArea()) {
            logger.printAdditionalInfo(cameraName + " noise level: " + cameraSettings.getNoiseLevel() + " and motion area size: " + differenceInPercentage + "%");
        }

        if (differenceInPercentage > cameraSettings.getMotionAreaPercent()) {
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

    public CameraMotionDetector on(Camera camera) throws MalformedURLException {
        this.cameraName = camera.getCameraName();
        this.sourceUrl = new URL(camera.getJpegUrl());
        this.cameraSettings = camera.getCameraSettings();
        return this;
    }
}
