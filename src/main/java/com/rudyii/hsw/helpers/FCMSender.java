package com.rudyii.hsw.helpers;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hsw.enums.FcmMessageEnum;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.rudyii.hsw.enums.FcmMessageEnum.FAIL;
import static com.rudyii.hsw.enums.FcmMessageEnum.SUCCESS;

@Component
public class FCMSender {
    public static final String TYPE_TO = "to";  // Use for single devices, device groups and topics
    private static final String TYPE_CONDITION = "condition"; // Use for Conditions
    private static final String URL_SEND = "https://fcm.googleapis.com/fcm/send";
    private static final Gson gson = new Gson();

    @Value("${fcm.server.key}")
    private String fcmServerKey;

    public FcmMessageEnum sendData(String recipientType, String recipientToken, FcmMessage message) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, null, message);
    }

    public FcmMessageEnum sendData(String recipientType, String recipientToken, FcmMessage message, Object notificationObject) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, notificationObject, message);
    }

    private FcmMessageEnum sendNotificationAndData(String recipientType, String recipientToken, Object notificationObject, FcmMessage message) throws IOException {
        FcmMessageEnum result = null;
        if (recipientType.equals(TYPE_TO) || recipientType.equals(TYPE_CONDITION)) {
            JsonObject recipientDetails = new JsonObject();
            recipientDetails.addProperty(recipientType, recipientToken);
            result = sendFcmMessage(recipientToken, message);
        }
        return result;
    }

    @SneakyThrows
    private FcmMessageEnum sendFcmMessage(String recipientToken, FcmMessage message) {
        String result = FirebaseMessaging.getInstance().send(Message.builder()
                .setToken(recipientToken)
                .putData("messageType", message.getMessageType().name())
                .putData("publishedAt", message.getPublishedAt() + "")
                .putData("serverKey", message.getServerKey())
                .putData("serverAlias", message.getServerAlias())
                .putData("by", message.getBy())
                .build());
        return result != null ? SUCCESS : FAIL;
    }
}
