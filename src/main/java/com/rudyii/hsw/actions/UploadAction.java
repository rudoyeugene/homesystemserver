package com.rudyii.hsw.actions;

import com.google.common.net.MediaType;
import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.objects.events.UploadEvent;
import com.rudyii.hsw.providers.StorageProvider;
import com.rudyii.hsw.services.internet.IspService;
import com.rudyii.hsw.services.system.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Component
@Scope(value = "prototype")
public class UploadAction extends InternetBasedAction implements Runnable {
    private final StorageProvider storageProvider;
    private final EventService eventService;
    private File uploadCandidate;
    private BufferedImage image;
    private String cameraName;

    @Autowired
    public UploadAction(StorageProvider storageProvider, IspService ispService, EventService eventService) {
        super(ispService);
        this.storageProvider = storageProvider;
        this.eventService = eventService;
    }

    public UploadAction withUploadCandidate(File uploadCandidate) {
        this.uploadCandidate = uploadCandidate;
        return this;
    }

    @Override
    public void run() {
        ensureInternetIsAvailable();
        uploadFile();
    }

    @Async
    void uploadFile() {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(uploadCandidate));
            eventService.publish(UploadEvent.builder()
                    .cameraName(cameraName)
                    .fileName(uploadCandidate.getName())
                    .videoUrl(storageProvider.putData(uploadCandidate.getName(), MediaType.MP4_VIDEO, in))
                    .image(image)
                    .build());
            uploadCandidate.delete();
        } catch (Exception e) {
            log.error("Upload to FAILED!", e);
        }
    }

    public UploadAction andImage(BufferedImage image) {
        this.image = image;

        return this;
    }

    public UploadAction withCameraName(String cameraName) {
        this.cameraName = cameraName;

        return this;
    }
}
