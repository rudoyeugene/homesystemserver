package com.rudyii.hsw.motion;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.events.CameraRebootEvent;
import com.rudyii.hsw.events.MotionDetectedEvent;
import com.rudyii.hsw.objects.Camera;
import com.rudyii.hsw.services.EventService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

import static com.rudyii.hsw.configuration.OptionsService.SHOW_MOTION_AREA;

@Component
@Scope(value = "prototype")
public class CameraMotionDetector {
    private static Logger LOG = LogManager.getLogger(CameraMotionDetector.class);
    private final OptionsService optionsService;

    private EventService eventService;
    private BufferedImage previousImage, currentImage, motionObject;
    private URL sourceUrl;
    private Camera camera;
    private boolean enabled = false;
    private boolean eventFired = false;

    @Autowired
    public CameraMotionDetector(EventService eventService, OptionsService optionsService) {
        this.eventService = eventService;
        this.optionsService = optionsService;
    }

    @Async
    public void start() throws Exception {
        this.enabled = true;
        LOG.info("Started CameraMotionDetector for " + camera.getName());

        try {
            this.previousImage = ImageIO.read(sourceUrl);
        } catch (Exception e) {
            LOG.error("Failed to get previous image from camera: " + camera.getName());
            stop();
        }
        try {
            this.currentImage = ImageIO.read(sourceUrl);
        } catch (Exception e) {
            LOG.error("Failed to get current image from camera: " + camera.getName());
            stop();
        }

        startDetection();
    }


    @Async
    public void stop() {
        this.enabled = false;
        LOG.info("Stopping CameraMotionDetector for " + camera.getName());
        Thread.currentThread().interrupt();
    }

    private void startDetection() throws InterruptedException {
        while (enabled) {
            this.previousImage = currentImage;
            try {
                this.motionObject = new BufferedImage(previousImage.getWidth(), previousImage.getHeight(), previousImage.getType());
                this.currentImage = ImageIO.read(sourceUrl);
                detect();
            } catch (Exception e) {
                LOG.error("Failed to get current image from camera: " + camera.getName());
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
            LOG.error("Images dimensions mismatch: previous image size = "
                    + previousImageWidth + "x" + previousImageHeight + " while current image size = "
                    + currentImageWidth + "x" + previousImageHeight + " on camera: " + camera.getName());
            return;
        }

        long diff = 0;
        for (int y = 0; y < previousImageHeight; y++) {
            for (int x = 0; x < previousImageWidth; x++) {
                int rgb1 = previousImage.getRGB(x, y);
                int rgb2 = currentImage.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = (rgb1) & 0xff;

                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = (rgb2) & 0xff;

                if ((Math.abs(r1 - r2) > noiseLevel()) && (Math.abs(g1 - g2) > noiseLevel()) && (Math.abs(b1 - b2) > noiseLevel())) {
                    motionObject.setRGB(x, y, rgb2);
                    diff++;
                }
            }
        }

        double imageSize = previousImageWidth * previousImageHeight;
        double differenceInPercentage = (100 * diff) / imageSize;

        if (showMotionArea()) {
            System.out.println(camera.getName() + " noise level: " + noiseLevel() + " and motion area size: " + differenceInPercentage + "%");
        }

        if (differenceInPercentage > motionAreaSize()) {
            LOG.info("Motion detected on " + camera.getName() + " with motion area size : " + differenceInPercentage + "%");
            eventService.publish(new MotionDetectedEvent(camera.getName(), differenceInPercentage, currentImage, motionObject));
        }
    }

    private void fireRebootEvent() {
        if (!eventFired) {
            LOG.error("Firing reboot event for camera: " + camera.getName());
            this.eventFired = true;
            eventService.publish(new CameraRebootEvent(camera.getName()));
        }
    }

    private boolean showMotionArea() {
        return (boolean) optionsService.getOption(SHOW_MOTION_AREA);
    }

    private long motionAreaSize() {
        return ((long) optionsService.getCameraOptions(camera.getName()).get("motionArea"));
    }

    private long interval() {
        return (long) optionsService.getCameraOptions(camera.getName()).get("interval");
    }

    private long noiseLevel() {
        return (long) optionsService.getCameraOptions(camera.getName()).get("noiseLevel");
    }


    public CameraMotionDetector onCamera(Camera camera) throws MalformedURLException {
        this.camera = camera;
        this.sourceUrl = new URL(camera.getJpegUrl());

        return this;
    }
}
