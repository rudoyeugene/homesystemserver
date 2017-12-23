package com.rudyii.hsw.actions;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by jack on 12.03.17.
 */
public class FCMSender {
    private static Logger LOG = LogManager.getLogger(FCMSender.class);

    private String endpoint;
    private String serverKey;
    private Connection connection;

    @Autowired
    public FCMSender(Connection connection) {
        this.connection = connection;
    }

    @Async
    public void sendMessage(String subject, ArrayList<String> body) {
        getRecipients().forEach((String recipient) -> {

            JSONObject message = new JSONObject();
            message.put("to", recipient);
            message.put("priority", "high");

            JSONObject messageData = new JSONObject();
            messageData.put("title", subject);
            messageData.put("body", body);

            message.put("data", messageData);

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();

            try {
                HttpPost request = new HttpPost(endpoint);
                StringEntity params = new StringEntity(message.toString());
                request.addHeader("Content-Type", "application/json");
                request.addHeader("Authorization", "key=" + serverKey);

                request.setEntity(params);
                CloseableHttpResponse response = httpClient.execute(request);
                LOG.info(response);
            } catch (Exception e) {
                LOG.error("Failed to send the notification: ", e);
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private ArrayList<String> getRecipients() {
        ArrayList<String> recipientTokens = new ArrayList<>();
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT TOKEN FROM FCM_USERS");
            while (resultSet.next()) {
                recipientTokens.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            LOG.error("Failed to get the recipient from the DB: ", e);
        }

        return recipientTokens;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

}
