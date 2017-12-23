package com.rudyii.hsw.helpers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Created by jack on 12.02.17.
 */
@Component
public class OutputController {

    @Value("${debug.enabled}")
    private boolean enableRedirect;

    @PostConstruct
    private void redirect() throws IOException {
        if (enableRedirect) {
            File debugLog = new File("logs/HomeSystemWebDEBUG.log");

            if (!debugLog.exists()) {
                debugLog.createNewFile();
            }

            System.setOut(new LoggingOutputStream(debugLog.getCanonicalPath()));
            System.setErr(new LoggingOutputStream(debugLog.getCanonicalPath()));
        }
    }
}