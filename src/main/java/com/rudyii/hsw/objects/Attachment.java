package com.rudyii.hsw.objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Attachment {
    private String name;
    private byte[] data;
    private String mimeType;
}
