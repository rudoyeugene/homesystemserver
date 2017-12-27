package com.rudyii.hsw.helpers;

import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FCMSender {
    public static final String TYPE_TO = "to";  // Use for single devices, device groups and topics
    public static final String TYPE_CONDITION = "condition"; // Use for Conditions
    private static final String URL_SEND = "https://fcm.googleapis.com/fcm/send";

    @Value("${fcm.server.key}")
    private String FCM_SERVER_KEY;

    public String sendNotification(String type, String typeParameter, JsonObject notificationObject) throws IOException {
        return sendNotifictaionAndData(type, typeParameter, notificationObject, null);
    }

    public String sendData(String type, String typeParameter, JsonObject dataObject) throws IOException {
        return sendNotifictaionAndData(type, typeParameter, null, dataObject);
    }

    public String sendNotifictaionAndData(String type, String typeParameter, JsonObject notificationObject, JsonObject dataObject) throws IOException {
        String result = null;
        if (type.equals(TYPE_TO) || type.equals(TYPE_CONDITION)) {
            JsonObject sendObject = new JsonObject();
            sendObject.addProperty(type, typeParameter);
            result = sendFcmMessage(sendObject, notificationObject, dataObject);
        }
        return result;
    }

    public String sendTopicData(String topic, JsonObject dataObject) throws IOException {
        return sendData(TYPE_TO, "/topics/" + topic, dataObject);
    }

    public String sendTopicNotification(String topic, JsonObject notificationObject) throws IOException {
        return sendNotification(TYPE_TO, "/topics/" + topic, notificationObject);
    }

    public String sendTopicNotificationAndData(String topic, JsonObject notificationObject, JsonObject dataObject) throws IOException {
        return sendNotifictaionAndData(TYPE_TO, "/topics/" + topic, notificationObject, dataObject);
    }

    private String sendFcmMessage(JsonObject sendObject, JsonObject notificationObject, JsonObject dataObject) throws IOException {
        HttpPost httpPost = new HttpPost(URL_SEND);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "key=" + FCM_SERVER_KEY);

        if (notificationObject != null) sendObject.add("notification", notificationObject);
        if (dataObject != null) sendObject.add("data", dataObject);

        String data = sendObject.toString();

        StringEntity entity = new StringEntity(data);
        httpPost.setEntity(entity);

        HttpClient httpClient = HttpClientBuilder.create().build();

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response = (String) httpClient.execute(httpPost, responseHandler);

        return response;
    }
}
