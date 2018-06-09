package com.rudyii.hsw.events;

import java.awt.image.BufferedImage;

public class UploadEvent extends EventBase {
    private String fileName;

    private BufferedImage image;
    private String url;

    public UploadEvent(String fileName, BufferedImage image, String url) {
        this.fileName = fileName;
        this.image = image;
        this.url = url;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
