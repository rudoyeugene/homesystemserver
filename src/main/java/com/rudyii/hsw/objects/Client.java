package com.rudyii.hsw.objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Client {
    private Boolean hourlyReportMuted;
    private Boolean notificationsMuted;
    private String email;
    private String device;
    private String appVersion;
    private String token;
    private String userId;
    private String notificationType;
}
