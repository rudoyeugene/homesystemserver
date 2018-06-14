package com.rudyii.hsw.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by jack on 06.06.17.
 */
@Component
public class EmailDetailsProvider {

    @Value("#{hswProperties['mail.smtp.port']}")
    private Integer smptPort;

    @Value("#{hswProperties['mail.smtp.host']}")
    private String smptServer;

    @Value("#{hswProperties['mail.username']}")
    private String username;

    @Value("#{hswProperties['mail.password']}")
    private String password;

    @Value("#{hswProperties['mail.recipients']}")
    private String recipients;

    @Value("#{hswProperties['mail.admin']}")
    private String admin;

    public Integer getSmptPort() {
        return smptPort;
    }

    public String getSmptServer() {
        return smptServer;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRecipients() {
        return recipients;
    }

    public String getAdmin() {
        return admin;
    }
}
