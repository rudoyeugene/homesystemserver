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
public class MotionToNotifyEvent extends EventBase {
    private long eventId;
    private URL snapshotUrl;
    private String cameraName;
    private Integer motionArea;
    private BufferedImage currentImage, motionObject;
}
