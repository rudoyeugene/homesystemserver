package com.rudyii.hsw.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hs.common.objects.message.MessageBase;
import com.rudyii.hsw.enums.FcmMessageEnum;
import com.rudyii.hsw.objects.FcmResponse;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

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

    public FcmMessageEnum sendData(String recipientType, String recipientToken, MessageBase messageBase) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, null, messageBase);
    }

    public FcmMessageEnum sendData(String recipientType, String recipientToken, MessageBase messageBase, Object notificationObject) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, notificationObject, messageBase);
    }

    private FcmMessageEnum sendNotificationAndData(String recipientType, String recipientToken, Object notificationObject, MessageBase messageBase) throws IOException {
        FcmMessageEnum result = null;
        if (recipientType.equals(TYPE_TO) || recipientType.equals(TYPE_CONDITION)) {
            JsonObject recipientDetails = new JsonObject();
            recipientDetails.addProperty(recipientType, recipientToken);
            result = sendFcmMessage(recipientToken, messageBase);
        }
        return result;
    }

    @SneakyThrows
    private FcmMessageEnum sendFcmMessage(String recipientToken, MessageBase messageBase) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey);
        HttpEntity<FcmMessage> entity = new HttpEntity<>(FcmMessage.builder()
                .to(recipientToken)
                .data(messageBase)
                .build(), headers);

        URI uri = new URI(URL_SEND);
        ResponseEntity<FcmResponse> result = restTemplate.postForEntity(uri, entity, FcmResponse.class);
        return result.getBody().getSuccess() == 1 ? SUCCESS : FAIL;
    }

    @Data
    @Builder
    private static class FcmMessage {
        private String to;
        private MessageBase data;
    }
}
