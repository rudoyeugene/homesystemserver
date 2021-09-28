package com.rudyii.hsw.services.notification;

import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hs.common.type.MessageType;
import com.rudyii.hs.common.type.NotificationType;
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

import static com.rudyii.hs.common.type.NotificationType.*;

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
        FcmMessage.FcmMessageBuilder fcmMessageBuilder = fillWithBasicData(event.getEventId());

        if (event instanceof SystemStateChangedEvent) {
            SystemStateChangedEvent armedEvent = (SystemStateChangedEvent) event;
            sendFcmMessage(fcmMessageBuilder.by(armedEvent.getBy()).messageType(MessageType.STATE_CHANGED).build(), ALL);

        } else if (event instanceof MotionToNotifyEvent) {
            sendFcmMessage(fcmMessageBuilder.messageType(MessageType.MOTION_DETECTED).build(), MOTION_DETECTED);

        } else if (event instanceof UploadEvent) {
            sendFcmMessage(fcmMessageBuilder.messageType(MessageType.RECORD_UPLOADED).build(), VIDEO_RECORDED);

        } else if (event instanceof IspEvent) {
            sendFcmMessage(fcmMessageBuilder.messageType(MessageType.ISP_CHANGED).build(), ALL);

        } else if (event instanceof CameraRebootEvent) {
            sendFcmMessage(fcmMessageBuilder.messageType(MessageType.CAMERA_REBOOTED).build(), ALL);

        } else if (event instanceof SimpleWatcherEvent) {
            sendFcmMessage(fcmMessageBuilder.messageType(MessageType.SIMPLE_WATCHER_FIRED).build(), ALL);
        }
    }

    private FcmMessage.FcmMessageBuilder fillWithBasicData(Long eventId) {
        return FcmMessage.builder()
                .publishedAt(eventId)
                .serverAlias(serverKeyService.getServerAlias())
                .serverKey(serverKeyService.getServerKey().toString());
    }

    private void notifyServerStarted() {
        sendFcmMessage(fillWithBasicData(System.currentTimeMillis()).messageType(MessageType.SERVER_START_STOP).build(), ALL);
    }

    private void notifyServerStopped() {
        sendFcmMessage(fillWithBasicData(System.currentTimeMillis()).messageType(MessageType.SERVER_START_STOP).build(), ALL);
    }

    private void sendFcmMessage(FcmMessage message, NotificationType serverNotificationType) {
        clientsService.getClients().forEach(client -> {
            NotificationType clientNotificationType = client.getNotificationType();
            String email = client.getEmail();
            String token = client.getToken();

            if (clientNotificationType.equals(serverNotificationType) || serverNotificationType.equals(ALL)) {
                notificationsService.sendFcmMessage(token, message);
            } else {
                log.warn("{} won't be notified, client notification type: {} != server notification type: {} and server is not ALL",
                        email, serverNotificationType, clientNotificationType);
            }
        });
    }
}