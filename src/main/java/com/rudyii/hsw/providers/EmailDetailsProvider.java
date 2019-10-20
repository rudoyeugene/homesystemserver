package com.rudyii.hsw.providers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
}
