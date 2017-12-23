package com.rudyii.hsw.events;

import java.awt.image.BufferedImage;

public class MotionDetectedEvent extends EventBase {
    private String cameraName;
    private Double motionArea;
    private BufferedImage currentImage, motionObject;

    public MotionDetectedEvent(String cameraName, Double motionArea, BufferedImage currentImage, BufferedImage motionObject) {
        this.cameraName = cameraName;
        this.motionArea = motionArea;
        this.currentImage = currentImage;
        this.motionObject = motionObject;
        System.out.println("Motion detected on camera: " + cameraName + " and motion area: " + motionArea + "%");
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    public BufferedImage getMotionObject() {
        return motionObject;
    }

    public String getCameraName() {
        return cameraName;
    }

    public Double getMotionArea() {
        return motionArea;
    }
}
