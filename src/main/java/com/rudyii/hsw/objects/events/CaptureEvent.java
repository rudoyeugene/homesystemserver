package com.rudyii.hsw.objects.events;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created by jack on 07.10.16.
 */
public class CaptureEvent extends EventBase {
    private File uploadCandidate;
    private BufferedImage image;

    public CaptureEvent(File uploadCandidate, BufferedImage image) {
        this.uploadCandidate = uploadCandidate;
        this.image = image;
        System.out.println("With upload candidate: " + uploadCandidate.getName());
    }

    public BufferedImage getImage() {
        return image;
    }

    public File getUploadCandidate() {
        return uploadCandidate;
    }
}
