package com.rudyii.hsw.objects.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.awt.image.BufferedImage;
import java.net.URL;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UploadEvent extends EventBase {
    private String cameraName;
    private URL videoUrl;
    private String fileName;
    private BufferedImage image;
}
