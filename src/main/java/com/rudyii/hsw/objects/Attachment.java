package com.rudyii.hsw.objects;

public class Attachment {
    private final String name;
    private final byte[] data;
    private final String mimeType;

    @SuppressWarnings("SameParameterValue")
    public Attachment(String name, byte[] data, String mimeType) {
        this.name = name;
        this.data = data;
        this.mimeType = mimeType;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getMimeType() {
        return this.mimeType;
    }
}
