package com.rudyii.hsw.events;

public class UploadEvent extends EventBase {
    private String fileName;
    private String url;

    public UploadEvent(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
