package com.rudyii.hsw.services;

import com.rudyii.hsw.actions.base.ActionsFactory;
import com.rudyii.hsw.objects.events.CaptureEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Created by jack on 18.01.17.
 */
@Service
public class UploadService {
    private static Logger LOG = LogManager.getLogger(UploadService.class);

    private ActionsFactory actionsFactory;

    @Autowired
    public UploadService(ActionsFactory actionsFactory) {
        this.actionsFactory = actionsFactory;
    }

    @Async
    @EventListener(CaptureEvent.class)
    public void onEvent(CaptureEvent event) {
        if (event.getUploadCandidate() != null && event.getUploadCandidate().exists()) {
            LOG.info("Invoking upload process for file: " + event.getUploadCandidate().getAbsolutePath());
            actionsFactory.addToQueueDropboxUploadAction(event.getUploadCandidate(), event.getImage());

        } else {
            LOG.warn("File: " + event.getUploadCandidate().getAbsolutePath() + " does not exist. Upload skipped");
        }
    }
}


