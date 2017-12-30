package com.rudyii.hsw.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hsw.enums.FcmMessageEnum;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.rudyii.hsw.enums.FcmMessageEnum.FAIL;
import static com.rudyii.hsw.enums.FcmMessageEnum.SUCCESS;

@Component
public class FCMSender {
    public static final String TYPE_TO = "to";  // Use for single devices, device groups and topics
    public static final String TYPE_CONDITION = "condition"; // Use for Conditions
    private static final String URL_SEND = "https://fcm.googleapis.com/fcm/send";
    private static Logger LOG = LogManager.getLogger(FCMSender.class);

    @Value("${fcm.server.key}")
    private String fcmServerKey;

    public FcmMessageEnum sendNotification(String type, String typeParameter, JsonObject notificationObject) throws IOException {
        return sendNotificationAndData(type, typeParameter, notificationObject, null);
    }

    public FcmMessageEnum sendData(String recipientType, String recipientToken, JsonObject messageData) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, null, messageData);
    }

    public FcmMessageEnum sendNotificationAndData(String recipientType, String recipientToken, JsonObject notificationObject, JsonObject messageData) throws IOException {
        FcmMessageEnum result = null;
        if (recipientType.equals(TYPE_TO) || recipientType.equals(TYPE_CONDITION)) {
            JsonObject recipientDetails = new JsonObject();
            recipientDetails.addProperty(recipientType, recipientToken);
            result = sendFcmMessage(recipientDetails, notificationObject, messageData);
        }
        return result;
    }

    public FcmMessageEnum sendTopicData(String topic, JsonObject dataObject) throws IOException {
        return sendData(TYPE_TO, "/topics/" + topic, dataObject);
    }

    public FcmMessageEnum sendTopicNotification(String topic, JsonObject notificationObject) throws IOException {
        return sendNotification(TYPE_TO, "/topics/" + topic, notificationObject);
    }

    public FcmMessageEnum sendTopicNotificationAndData(String topic, JsonObject notificationObject, JsonObject dataObject) throws IOException {
        return sendNotificationAndData(TYPE_TO, "/topics/" + topic, notificationObject, dataObject);
    }

    private FcmMessageEnum sendFcmMessage(JsonObject recipientDetails, JsonObject notificationObject, JsonObject messageData) throws IOException {
        HttpPost httpPost = new HttpPost(URL_SEND);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "key=" + fcmServerKey);

        if (notificationObject != null) recipientDetails.add("notification", notificationObject);
        if (messageData != null) recipientDetails.add("data", messageData);

        String data = recipientDetails.toString();

        StringEntity entity = new StringEntity(data);
        httpPost.setEntity(entity);

        HttpClient httpClient = HttpClientBuilder.create().build();

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response = (String) httpClient.execute(httpPost, responseHandler);

        Gson gson = new Gson();
        Map<String, Object> result = gson.fromJson(response, Map.class);

        return (Double) result.get("failure") == 0 ? SUCCESS : FAIL;
    }
}
