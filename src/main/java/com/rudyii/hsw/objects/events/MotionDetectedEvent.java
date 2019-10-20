package com.rudyii.hsw.objects.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.awt.image.BufferedImage;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MotionDetectedEvent extends EventBase {
    private String cameraName;
    private Double motionArea;
    private BufferedImage currentImage, motionObject;
}
