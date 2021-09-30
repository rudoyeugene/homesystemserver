package com.rudyii.hsw.services.messaging;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.rudyii.hs.common.objects.logs.LogBase;
import com.rudyii.hs.common.objects.logs.StartStopLog;
import com.rudyii.hs.common.objects.logs.StateChangedLog;
import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hs.common.type.LogType;
import com.rudyii.hs.common.type.MessageType;
import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hs.common.type.ServerStateType;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.services.ClientsService;
import com.rudyii.hsw.services.system.ServerKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;
import static com.rudyii.hs.common.type.NotificationType.*;

@Service
@Slf4j
@AllArgsConstructor
public class FirebaseMessageService {
    private final ClientsService clientsService;
    private final ServerKeyService serverKeyService;
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final ThreadPoolTaskExecutor hswExecutor;

    @PostConstruct
    public void init() {
        notifyServerStarted();
    }

    @PreDestroy
    private void destroy() {
        notifyServerStopped();
    }

    @Async
    public void sendMessageForLog(LogBase logBase) {
        FcmMessage.FcmMessageBuilder fcmMessageBuilder = fillWithBasicData(logBase.getEventId());

        switch (logBase.getLogType()) {
            case STATE_CHANGED:
                StateChangedLog stateChangedLog = (StateChangedLog) logBase;
                sendFcmMessage(fcmMessageBuilder.by(stateChangedLog.getBy()).messageType(MessageType.STATE_CHANGED).build(), ALL);
                break;
            case MOTION_DETECTED:
                sendFcmMessage(fcmMessageBuilder.messageType(MessageType.MOTION_DETECTED).build(), MOTION_DETECTED);
                break;
            case RECORD_UPLOADED:
                sendFcmMessage(fcmMessageBuilder.messageType(MessageType.RECORD_UPLOADED).build(), VIDEO_RECORDED);
                break;
            case ISP_CHANGED:
                sendFcmMessage(fcmMessageBuilder.messageType(MessageType.ISP_CHANGED).build(), ALL);
                break;
            case CAMERA_REBOOTED:
                sendFcmMessage(fcmMessageBuilder.messageType(MessageType.CAMERA_REBOOTED).build(), ALL);
                break;
            case SIMPLE_WATCHER_FIRED:
                sendFcmMessage(fcmMessageBuilder.messageType(MessageType.SIMPLE_WATCHER_FIRED).build(), ALL);
                break;
        }
    }

    private FcmMessage.FcmMessageBuilder fillWithBasicData(Long eventId) {
        return FcmMessage.builder()
                .publishedAt(eventId)
                .serverAlias(serverKeyService.getServerAlias())
                .serverKey(serverKeyService.getServerKey().toString());
    }

    private void notifyServerStarted() {
        long now = System.currentTimeMillis();
        firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(now)).setValueAsync(StartStopLog.builder()
                .logType(LogType.SERVER_START_STOP)
                .serverState(ServerStateType.STARTED)
                .eventId(now)
                .build()).addListener(() -> sendFcmMessage(fillWithBasicData(now).messageType(MessageType.SERVER_START_STOP).build(), ALL), hswExecutor);
    }

    private void notifyServerStopped() {
        long now = System.currentTimeMillis();
        firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(now)).setValueAsync(StartStopLog.builder()
                .logType(LogType.SERVER_START_STOP)
                .serverState(ServerStateType.STOPPED)
                .eventId(now)
                .build()).addListener(() -> sendFcmMessage(fillWithBasicData(now).messageType(MessageType.SERVER_START_STOP).build(), ALL), hswExecutor);
    }

    private void sendFcmMessage(FcmMessage message, NotificationType serverNotificationType) {
        clientsService.getClients().forEach(client -> {
            NotificationType clientNotificationType = client.getNotificationType();
            String email = client.getEmail();
            String token = client.getToken();

            if (clientNotificationType.equals(serverNotificationType)
                    || clientNotificationType.equals(ALL)
                    || serverNotificationType.equals(ALL)) {
                Message firebaseMessage = Message.builder()
                        .setToken(token)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .putData("messageType", message.getMessageType().name())
                        .putData("publishedAt", message.getPublishedAt() + "")
                        .putData("serverKey", message.getServerKey())
                        .putData("serverAlias", message.getServerAlias())
                        .putData("by", message.getBy() == null ? "system" : message.getBy())
                        .build();
                try {
                    String result = FirebaseMessaging.getInstance().send(firebaseMessage);
                    log.info(result);
                } catch (Exception e) {
                    log.error("Failed to send FCM message to {}", email, e);
                }
            } else {
                log.warn("{} won't be notified, client notification type: {} != server notification type: {} and server is not ALL",
                        email, serverNotificationType, clientNotificationType);
            }
        });
    }
}
