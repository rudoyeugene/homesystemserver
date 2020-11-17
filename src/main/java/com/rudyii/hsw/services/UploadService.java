package com.rudyii.hsw.services;

import com.rudyii.hsw.actions.base.ActionsFactory;
import com.rudyii.hsw.objects.events.CaptureEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UploadService {
    private final ActionsFactory actionsFactory;

    @Autowired
    public UploadService(ActionsFactory actionsFactory) {
        this.actionsFactory = actionsFactory;
    }

    @Async
    @EventListener(CaptureEvent.class)
    public void onEvent(CaptureEvent event) {
        if (event.getUploadCandidate() != null && event.getUploadCandidate().exists()) {
            log.info("Invoking upload process for file: {}", event.getUploadCandidate().getAbsolutePath());
            actionsFactory.orderUploadAction(event.getCameraName(), event.getUploadCandidate(), event.getImage());

        } else {
            log.warn("File: {} does not exist. Uploading skipped", event.getUploadCandidate());
        }
    }
}


