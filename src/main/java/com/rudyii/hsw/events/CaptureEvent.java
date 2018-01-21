package com.rudyii.hsw.events;

import java.io.File;

/**
 * Created by jack on 07.10.16.
 */
public class CaptureEvent extends EventBase {
    private File uploadCandidate;

    public CaptureEvent(File uploadCandidate) {
        this.uploadCandidate = uploadCandidate;
        System.out.println("With upload candidate: " + uploadCandidate.getName());
    }

    public File getUploadCandidate() {
        return uploadCandidate;
    }
}
