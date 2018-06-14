package com.rudyii.hsw.objects.events;

/**
 * Created by jack on 04.02.17.
 */
public class CameraRebootEvent extends EventBase {
    private String cameraName;

    public CameraRebootEvent(String cameraName) {
        this.cameraName = cameraName;
        System.out.println("For camera: " + cameraName);
    }

    public String getCameraName() {
        return cameraName;
    }
}
