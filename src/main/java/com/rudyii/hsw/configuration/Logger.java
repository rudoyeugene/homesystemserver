package com.rudyii.hsw.configuration;

import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Logger {
    private final FirebaseGlobalSettingsService globalSettingsService;

    @Async
    public void printAdditionalInfo(String message) {
        if (globalSettingsService.getGlobalSettings().isVerboseOutput()) {
            System.out.println(message);
            log.info("VERBOSE_OUTPUT:> {}", message);
        }
    }
}
