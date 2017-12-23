package com.rudyii.hsw.events;

import java.io.File;

/**
 * Created by jack on 07.10.16.
 */
public class CaptureEvent extends EventBase {
    private File uploadCandidate;
    private boolean toDropbox = false;
    private boolean toGoogleDrive = false;

    public CaptureEvent(File uploadCandidate, boolean toDropbox, boolean toGoogleDrive) {
        this.uploadCandidate = uploadCandidate;
        this.toDropbox = toDropbox;
        this.toGoogleDrive = toGoogleDrive;
        System.out.println("With upload candidate: " + uploadCandidate.getName());
    }

    public File getUploadCandidate() {
        return uploadCandidate;
    }

    public boolean isToDropbox() {
        return toDropbox;
    }

    public boolean isToGoogleDrive() {
        return toGoogleDrive;
    }
}
