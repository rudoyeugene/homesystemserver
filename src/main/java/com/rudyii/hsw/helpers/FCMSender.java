package com.rudyii.hsw.helpers;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.rudyii.hs.common.objects.message.FcmMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class FCMSender {

    public void sendData(String recipientToken, FcmMessage message) {
        sendNotificationAndData(recipientToken, message);
    }

    private void sendNotificationAndData(String recipientToken, FcmMessage message) {
        sendFcmMessage(recipientToken, message);
    }

    private void sendFcmMessage(String recipientToken, FcmMessage message) {
        Message firebaseMessage = Message.builder()
                .setToken(recipientToken)
                .putData("messageType", message.getMessageType().name())
                .putData("publishedAt", message.getPublishedAt() + "")
                .putData("serverKey", message.getServerKey())
                .putData("serverAlias", message.getServerAlias())
                .putData("by", message.getBy())
                .build();
        FirebaseMessaging.getInstance().sendAsync(firebaseMessage);
    }
}
