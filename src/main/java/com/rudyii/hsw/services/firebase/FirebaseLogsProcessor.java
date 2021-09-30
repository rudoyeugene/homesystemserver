package com.rudyii.hsw.services.firebase;

import com.rudyii.hs.common.objects.logs.*;
import com.rudyii.hs.common.type.LogType;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.services.messaging.FirebaseMessageService;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;

@Service
@AllArgsConstructor
public class FirebaseLogsProcessor {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final ThreadPoolTaskExecutor hswExecutor;
    private FirebaseMessageService firebaseMessageService;

    @EventListener({SystemStateChangedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionToNotifyEvent.class, UploadEvent.class, SimpleWatcherEvent.class})
    public void onEvent(EventBase event) {
        LogBase logBase = null;

        if (event instanceof SystemStateChangedEvent) {
            SystemStateChangedEvent armedEvent = (SystemStateChangedEvent) event;
            StateChangedLog stateChangedLog = StateChangedLog.builder()
                    .eventId(armedEvent.getEventId())
                    .logType(LogType.STATE_CHANGED)
                    .systemMode(armedEvent.getSystemMode())
                    .systemState(armedEvent.getSystemState())
                    .by(armedEvent.getBy())
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(stateChangedLog).addListener(() -> firebaseMessageService.sendMessageForLog(stateChangedLog), hswExecutor);

        } else if (event instanceof MotionToNotifyEvent) {
            MotionToNotifyEvent motionToNotifyEvent = (MotionToNotifyEvent) event;
            MotionLog motionLog = MotionLog.builder()
                    .eventId(motionToNotifyEvent.getEventId())
                    .logType(LogType.MOTION_DETECTED)
                    .imageUrl(motionToNotifyEvent.getSnapshotUrl().toString())
                    .cameraName(motionToNotifyEvent.getCameraName())
                    .motionArea(motionToNotifyEvent.getMotionArea())
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(motionLog).addListener(() -> firebaseMessageService.sendMessageForLog(motionLog), hswExecutor);

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;
            UploadLog uploadLog = UploadLog.builder()
                    .eventId(uploadEvent.getEventId())
                    .logType(LogType.RECORD_UPLOADED)
                    .videoUrl(uploadEvent.getVideoUrl().toString())
                    .cameraName(uploadEvent.getCameraName())
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(uploadLog).addListener(() -> firebaseMessageService.sendMessageForLog(uploadLog), hswExecutor);

        } else if (event instanceof IspEvent) {
            IspEvent ispEvent = (IspEvent) event;
            IspLog ispLog = IspLog.builder()
                    .eventId(ispEvent.getEventId())
                    .logType(LogType.ISP_CHANGED)
                    .ispIp(ispEvent.getExternalIp())
                    .ispName(ispEvent.getIspName())
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(ispLog).addListener(() -> firebaseMessageService.sendMessageForLog(ispLog), hswExecutor);

        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;
            CameraRebootLog cameraRebootLog = CameraRebootLog.builder()
                    .eventId(cameraRebootEvent.getEventId())
                    .logType(LogType.CAMERA_REBOOTED)
                    .cameraName(cameraRebootEvent.getCameraName())
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(cameraRebootLog).addListener(() -> firebaseMessageService.sendMessageForLog(cameraRebootLog), hswExecutor);

        } else if (event instanceof SimpleWatcherEvent) {
            SimpleWatcherEvent simpleWatcherEvent = (SimpleWatcherEvent) event;
            SimpleWatcherLog simpleWatcherLog = SimpleWatcherLog.builder()
                    .eventId(simpleWatcherEvent.getEventId())
                    .logType(LogType.SIMPLE_WATCHER_FIRED)
                    .originalText(simpleWatcherEvent.getNotificationText())
                    .base64EncodedText(Base64.getEncoder().encodeToString(simpleWatcherEvent.getNotificationText().getBytes(StandardCharsets.UTF_8)))
                    .build();
            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(event.getEventId())).setValueAsync(simpleWatcherLog).addListener(() -> firebaseMessageService.sendMessageForLog(simpleWatcherLog), hswExecutor);
        }
    }
}
