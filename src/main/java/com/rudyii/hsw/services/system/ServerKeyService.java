package com.rudyii.hsw.services.system;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

@Slf4j
@Service
public class ServerKeyService {
    private final ApplicationContext context;

    private UUID serverKey;

    @Value("#{hswProperties['server.alias']}")
    private String serverAlias;

    public ServerKeyService(ApplicationContext context) {
        this.context = context;
        readServerKey();
    }

    private void readServerKey() {
        String plainServerKey = null;
        try {
            plainServerKey = FileUtils.readFileToString(new File("conf/server.key"), Charset.defaultCharset());
        } catch (IOException ioe) {
            log.error("Failed to start, server key not found at {}", "conf/server.key");
            exitAppWithError();
        }
        try {
            this.serverKey = UUID.fromString(plainServerKey);
        } catch (IllegalArgumentException iae) {
            log.error("Unsupported server key found: '{}'", plainServerKey);
            exitAppWithError();
        }
    }

    private void exitAppWithError() {
        int exitCode = SpringApplication.exit(context, () -> 255);
        System.exit(exitCode);
    }

    public UUID getServerKey() {
        return serverKey;
    }

    public String getServerAlias() {
        return serverAlias;
    }
}
