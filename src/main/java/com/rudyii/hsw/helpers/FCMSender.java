package com.rudyii.hsw.helpers;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.rudyii.hs.common.objects.message.FcmMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FCMSender {
    public void sendData(String recipientToken, FcmMessage message) {
        sendNotificationAndData(recipientToken, message);
    }

    private void sendNotificationAndData(String recipientToken, FcmMessage message) {
        sendFcmMessage(recipientToken, message);
    }

    @SneakyThrows
    private void sendFcmMessage(String recipientToken, FcmMessage message) {
        log.info(FirebaseMessaging.getInstance().send(Message.builder()
                .setToken(recipientToken)
                .putData("messageType", message.getMessageType().name())
                .putData("publishedAt", message.getPublishedAt() + "")
                .putData("serverKey", message.getServerKey())
                .putData("serverAlias", message.getServerAlias())
                .putData("by", message.getBy())
                .build()));
    }
}
