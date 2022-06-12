package com.rudyii.hsw.objects.events;

import com.rudyii.hs.common.type.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.awt.image.BufferedImage;
import java.io.File;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CaptureEvent extends EventBase {
    private NotificationType notificationType;
    private String cameraName;
    private long eventId;
    private File uploadCandidate;
    private BufferedImage image;
}
