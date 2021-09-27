package com.rudyii.hsw.services.notification;

import com.rudyii.hs.common.objects.message.*;
import com.rudyii.hs.common.type.MessageType;
import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hs.common.type.ServerStateType;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.NotificationsService;
import com.rudyii.hsw.services.ClientsService;
import com.rudyii.hsw.services.system.ServerKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.rudyii.hs.common.type.NotificationType.ALL;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

@Slf4j
@Service
@AllArgsConstructor
public class FirebaseMessagesProcessor {
    private final ServerKeyService serverKeyService;
    private final NotificationsService notificationsService;
    private final ClientsService clientsService;

    @PostConstruct
    public void init() {
        notifyServerStarted();
    }

    @PreDestroy
    private void destroy() {
        notifyServerStopped();
    }

    @EventListener({SystemStateChangedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionToNotifyEvent.class, UploadEvent.class, SimpleWatcherEvent.class})
    public void onEvent(EventBase event) {
        if (event instanceof SystemStateChangedEvent) {
            SystemStateChangedEvent armedEvent = (SystemStateChangedEvent) event;
            StateChangedMessage stateChangedMessage = StateChangedMessage.builder()
                    .messageType(MessageType.STATE_CHANGED)
                    .systemMode(armedEvent.getSystemMode())
                    .systemState(armedEvent.getSystemState())
                    .by(armedEvent.getBy())
                    .build();

            fillWithBasicData(event.getEventId(), stateChangedMessage);
            sendFcmMessage(stateChangedMessage, ALL);

        } else if (event instanceof MotionToNotifyEvent) {
            MotionToNotifyEvent motionToNotifyEvent = (MotionToNotifyEvent) event;
            MotionDetectedMessage motionDetectedMessage = MotionDetectedMessage.builder()
                    .messageType(MessageType.MOTION_DETECTED)
                    .imageUrl(motionToNotifyEvent.getSnapshotUrl().toString())
                    .cameraName(motionToNotifyEvent.getCameraName())
                    .motionArea(motionToNotifyEvent.getMotionArea())
                    .build();

            fillWithBasicData(event.getEventId(), motionDetectedMessage);
            sendFcmMessage(motionDetectedMessage, NotificationType.MOTION_DETECTED);

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;
            VideoUploadedMessage videoUploadedMessage = VideoUploadedMessage.builder()
                    .messageType(MessageType.RECORD_UPLOADED)
                    .videoUrl(uploadEvent.getVideoUrl().toString())
                    .fileName(uploadEvent.getFileName())
                    .cameraName(uploadEvent.getCameraName())
                    .build();

            fillWithBasicData(event.getEventId(), videoUploadedMessage);
            sendFcmMessage(videoUploadedMessage, NotificationType.VIDEO_RECORDED);

        } else if (event instanceof IspEvent) {
            IspEvent ispEvent = (IspEvent) event;
            IspChangedMessage ispChangedMessage = IspChangedMessage.builder()
                    .messageType(MessageType.ISP_CHANGED)
                    .ispName(ispEvent.getIspName())
                    .externalIp(ispEvent.getExternalIp())
                    .build();

            fillWithBasicData(event.getEventId(), ispChangedMessage);
            sendFcmMessage(ispChangedMessage, ALL);

        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;
            CameraRebootMessage cameraRebootMessage = CameraRebootMessage.builder()
                    .messageType(MessageType.CAMERA_REBOOTED)
                    .cameraName(cameraRebootEvent.getCameraName())
                    .build();

            fillWithBasicData(event.getEventId(), cameraRebootMessage);
            sendFcmMessage(cameraRebootMessage, ALL);

        } else if (event instanceof SimpleWatcherEvent) {
            SimpleWatcherEvent simpleWatcherEvent = (SimpleWatcherEvent) event;
            SimpleWatchMessage simpleWatchMessage = SimpleWatchMessage.builder()
                    .messageType(MessageType.SIMPLE_WATCHER_FIRED)
                    .plainText(simpleWatcherEvent.getNotificationText())
                    .encodedText(Base64.getEncoder().encodeToString(simpleWatcherEvent.getNotificationText().getBytes(StandardCharsets.UTF_8)))
                    .build();

            fillWithBasicData(event.getEventId(), simpleWatchMessage);
            sendFcmMessage(simpleWatchMessage, ALL);
        }
    }

    private void fillWithBasicData(Long eventId, MessageBase messageBase) {
        messageBase.setPublishedAt(eventId);
        messageBase.setServerAlias(serverKeyService.getServerAlias());
        messageBase.setServerKey(serverKeyService.getServerKey().toString());
        messageBase.setBy(messageBase.getBy());
    }

    private void notifyServerStarted() {
        ServerStartedStoppedMessage serverStartedStoppedMessage = ServerStartedStoppedMessage.builder()
                .messageType(MessageType.SERVER_START_STOP)
                .pid(getPid())
                .serverState(ServerStateType.STARTED)
                .build();
        fillWithBasicData(System.currentTimeMillis(), serverStartedStoppedMessage);

        sendFcmMessage(serverStartedStoppedMessage, ALL);
    }

    private void notifyServerStopped() {
        ServerStartedStoppedMessage serverStartedStoppedMessage = ServerStartedStoppedMessage.builder()
                .messageType(MessageType.SERVER_START_STOP)
                .pid(getPid())
                .serverState(ServerStateType.STOPPED)
                .build();

        fillWithBasicData(System.currentTimeMillis(), serverStartedStoppedMessage);
        sendFcmMessage(serverStartedStoppedMessage, ALL);
    }

    private void sendFcmMessage(MessageBase messageBase, NotificationType notificationType) {
        clientsService.getClients().forEach(client -> {
            NotificationType clientNotificationType = client.getNotificationType();
            String email = client.getEmail();
            String device = client.getDevice();
            String appVersion = client.getAppVersion();
            String token = client.getToken();

            boolean shouldSendFcm = notificationType.equals(clientNotificationType) || ALL.equals(clientNotificationType);

            if (shouldSendFcm && tokenLooksGood(token)) {
                log.info("Ready to send message to the Client:" + email + " on device " + device + " with client version " + appVersion);

                notificationsService.sendFcmMessage(email, token, messageBase);
            } else {
                log.warn("Client:" + email + " on device " + device + " with client version " + appVersion + " is not interested in such type of notification: server - " + notificationType + ", client - " + clientNotificationType);
            }
        });
    }

    private boolean tokenLooksGood(String token) {
        return isNoneBlank(token);
    }
}
