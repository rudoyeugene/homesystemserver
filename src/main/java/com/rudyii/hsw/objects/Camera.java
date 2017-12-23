package com.rudyii.hsw.objects;

public class Camera {
    private String name;
    private String jpegUrl;
    private String rtspUrl;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRtspUrl() {
        return rtspUrl;
    }

    public void setRtspUrl(String mjpegStreamURL) {
        this.rtspUrl = mjpegStreamURL;
    }

    public String getJpegUrl() {
        return jpegUrl;
    }

    public void setJpegUrl(String jpegUrl) {
        this.jpegUrl = jpegUrl;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
