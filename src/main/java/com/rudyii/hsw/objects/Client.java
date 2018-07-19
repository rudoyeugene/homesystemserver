package com.rudyii.hsw.objects;

import java.util.HashMap;

public class Client {
    private Boolean hourlyReportMuted;
    private Boolean notificationsMuted;
    private String email;
    private String device;
    private String appVersion;
    private String token;
    private String userId;
    private String notificationType;


    public Client(String userId, HashMap<String, Object> userProperties) {
        this.userId = userId;
        this.device = (String) userProperties.get("device");
        this.appVersion = (String) userProperties.get("appVersion");
        this.token = (String) userProperties.get("token");
        this.notificationType = (String) userProperties.get("notificationType");
        this.notificationsMuted = Boolean.TRUE.equals(userProperties.get("notificationsMuted"));
        this.hourlyReportMuted = Boolean.TRUE.equals(userProperties.get("hourlyReportMuted"));
        this.email = (String) userProperties.get("email");
    }

    public Boolean isNotificationsMuted() {
        return notificationsMuted;
    }

    public Boolean isHourlyReportMuted() {
        return hourlyReportMuted;
    }

    public String getEmail() {
        return email;
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
    public String toString() {
        return "Client " + userId + " on the device " + device + " with app version " + appVersion + " added or updated";
    }
}
