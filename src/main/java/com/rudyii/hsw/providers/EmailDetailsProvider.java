package com.rudyii.hsw.providers;

import org.springframework.beans.factory.annotation.Value;

/**
 * Created by jack on 06.06.17.
 */
public class EmailDetailsProvider {

    @Value("${mail.smtp.port}")
    private Integer smptPort;

    @Value("${mail.smtp.host}")
    private String smptServer;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.recipients}")
    private String recipients;

    @Value("${mail.admin}")
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
