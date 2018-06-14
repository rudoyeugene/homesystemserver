package com.rudyii.hsw.objects;

import java.util.HashMap;

public class Client {
    private String device;
    private String appVersion;
    private String token;
    private String userId;
    private String notificationType;

    public Client(String userId, HashMap<String, String> userProperties) {
        this.userId = userId;
        this.device = userProperties.get("device");
        this.appVersion = userProperties.get("appVersion");
        this.token = userProperties.get("token");
        this.notificationType = userProperties.get("notificationType");
    }

    public String getDevice() {
        return device;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    @Override
    public String toString(){
        return "Client " + userId + " on the device " + device + " with app version " + appVersion + " added or updated";
    }
}
