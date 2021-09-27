package com.rudyii.hsw.services.firebase;

import com.rudyii.hs.common.objects.logs.*;
import com.rudyii.hs.common.type.LogType;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.*;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;

@Service
@AllArgsConstructor
public class FirebaseLogsProcessor {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;

    @EventListener({SystemStateChangedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionToNotifyEvent.class, UploadEvent.class, SimpleWatcherEvent.class})
    public void onEvent(EventBase event) {
        LogBase logBase = null;

        if (event instanceof SystemStateChangedEvent) {
            SystemStateChangedEvent armedEvent = (SystemStateChangedEvent) event;
            logBase = StateChangedLog.builder()
                    .eventId(armedEvent.getEventId())
                    .logType(LogType.STATE_CHANGED)
                    .systemMode(armedEvent.getSystemMode())
                    .systemState(armedEvent.getSystemState())
                    .by(armedEvent.getBy())
                    .build();

        } else if (event instanceof MotionToNotifyEvent) {
            MotionToNotifyEvent motionToNotifyEvent = (MotionToNotifyEvent) event;
            logBase = MotionLog.builder()
                    .eventId(motionToNotifyEvent.getEventId())
                    .logType(LogType.MOTION_DETECTED)
                    .imageUrl(motionToNotifyEvent.getSnapshotUrl().toString())
                    .cameraName(motionToNotifyEvent.getCameraName())
                    .motionArea(motionToNotifyEvent.getMotionArea())
                    .build();

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;
            logBase = UploadLog.builder()
                    .eventId(uploadEvent.getEventId())
                    .logType(LogType.RECORD_UPLOADED)
                    .videoUrl(uploadEvent.getVideoUrl().toString())
                    .cameraName(uploadEvent.getCameraName())
                    .build();

        } else if (event instanceof IspEvent) {
            IspEvent ispEvent = (IspEvent) event;
            logBase = IspLog.builder()
                    .eventId(ispEvent.getEventId())
                    .logType(LogType.ISP_CHANGED)
                    .ispIp(ispEvent.getExternalIp())
                    .ispName(ispEvent.getIspName())
                    .build();

        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;
            logBase = CameraRebootLog.builder()
                    .eventId(cameraRebootEvent.getEventId())
                    .logType(LogType.CAMERA_REBOOTED)
                    .cameraName(cameraRebootEvent.getCameraName())
                    .build();

        } else if (event instanceof SimpleWatcherEvent) {
            SimpleWatcherEvent simpleWatcherEvent = (SimpleWatcherEvent) event;
            logBase = SimpleWatcherLog.builder()
                    .eventId(simpleWatcherEvent.getEventId())
                    .logType(LogType.SIMPLE_WATCHER_FIRED)
                    .originalText(simpleWatcherEvent.getNotificationText())
                    .base64EncodedText(Base64.getEncoder().encodeToString(simpleWatcherEvent.getNotificationText().getBytes(StandardCharsets.UTF_8)))
                    .build();
        }

        if (logBase != null) {
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(logBase.getEventId())).setValueAsync(logBase);
        }
    }
}
