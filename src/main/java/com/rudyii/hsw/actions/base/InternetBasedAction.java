package com.rudyii.hsw.actions.base;

import com.rudyii.hsw.services.internet.IspService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class InternetBasedAction {
    private final IspService ispService;

    @Autowired
    public InternetBasedAction(IspService ispService) {
        this.ispService = ispService;
    }

    public void ensureInternetIsAvailable() {
        while (!ispService.internetIsAvailable()) {
            try {
                Thread.sleep(30000L);
                log.info("Internet is unavailable, waiting another 30 seconds...");
            } catch (InterruptedException e) {
                log.error("Unknown error occurred:", e);
            }
        }
    }
}
