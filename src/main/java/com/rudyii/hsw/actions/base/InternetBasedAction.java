package com.rudyii.hsw.actions.base;

import com.rudyii.hsw.services.IspService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jack on 06.06.17.
 */
public abstract class InternetBasedAction {
    private static Logger LOG = LogManager.getLogger(InternetBasedAction.class);
    private IspService ispService;

    @Autowired
    public InternetBasedAction(IspService ispService) {
        this.ispService = ispService;
    }

    public void ensureInternetIsAvailable() {
        while (!ispService.internetIsAvailable()) {
            try {
                Thread.sleep(30000L);
                LOG.info(getClass().getSimpleName() + ": Internet is unavailable, waiting another 30 seconds...");
            } catch (InterruptedException e) {
                LOG.error(e);
            }
        }
    }
}
