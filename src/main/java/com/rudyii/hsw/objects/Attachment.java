package com.rudyii.hsw.objects;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Attachment {
    private String name;
    private byte[] data;
    private String mimeType;
}
