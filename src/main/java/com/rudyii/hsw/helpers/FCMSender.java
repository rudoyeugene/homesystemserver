package com.rudyii.hsw.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.rudyii.hsw.enums.FcmMessageEnum;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

import static com.rudyii.hsw.enums.FcmMessageEnum.*;

@Component
public class FCMSender {
    public static final String TYPE_TO = "to";  // Use for single devices, device groups and topics
    private static final String TYPE_CONDITION = "condition"; // Use for Conditions
    private static final String URL_SEND = "https://fcm.googleapis.com/fcm/send";

    @Value("${fcm.server.key}")
    private String fcmServerKey;

    public FcmMessageEnum sendData(String recipientType, String recipientToken, JsonObject messageData) throws IOException {
        return sendNotificationAndData(recipientType, recipientToken, null, messageData);
    }

    private FcmMessageEnum sendNotificationAndData(String recipientType, String recipientToken, JsonObject notificationObject, JsonObject messageData) throws IOException {
        FcmMessageEnum result = null;
        if (recipientType.equals(TYPE_TO) || recipientType.equals(TYPE_CONDITION)) {
            JsonObject recipientDetails = new JsonObject();
            recipientDetails.addProperty(recipientType, recipientToken);
            result = sendFcmMessage(recipientDetails, notificationObject, messageData);
        }
        return result;
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
        String response = httpClient.execute(httpPost, responseHandler);

        return processResults(response);
    }

    private FcmMessageEnum processResults(String response) {
        Gson gson = new Gson();
        LinkedTreeMap<String, Object> result = gson.fromJson(response, LinkedTreeMap.class);
        ArrayList<Object> detailedResult = (ArrayList<Object>) result.get("results");
        LinkedTreeMap<String, Object> results = (LinkedTreeMap<String, Object>) detailedResult.get(0);

        String error = (String) results.get("error");

        if ("InvalidRegistration".equals(error)) {
            return WARNING;
        }

        return (Double) result.get("failure") == 0 ? SUCCESS : FAIL;
    }
}
