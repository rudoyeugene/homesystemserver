package com.rudyii.hsw.actions;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import com.rudyii.hsw.actions.base.Action;
import com.rudyii.hsw.events.UploadEvent;
import com.rudyii.hsw.services.EventService;
import com.rudyii.hsw.services.IspService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

/**
 * Created by jack on 06.06.17.
 */
@Component
@Scope(value = "prototype")
public class DropboxUploadAction implements Action {
    private static Logger LOG = LogManager.getLogger(DropboxUploadAction.class);

    private DbxClientV2 client;
    private File uploadCandidate;
    private IspService ispService;
    private EventService eventService;

    @Autowired
    public DropboxUploadAction(DbxClientV2 client, IspService ispService, EventService eventService) {
        this.client = client;
        this.ispService = ispService;
        this.eventService = eventService;
    }

    public DropboxUploadAction withUploadCandidate(File uploadCandidate) {
        this.uploadCandidate = uploadCandidate;
        return this;
    }

    @Override
    public boolean fireAction() {
        if (ispService.internetIsAvailable()) {
            uploadFile();
            return true;
        } else {
            return false;
        }
    }

    @Async
    void uploadFile() {

        try {
            FileInputStream in = new FileInputStream(uploadCandidate);

            FileMetadata metadata = client.files().uploadBuilder("/" + uploadCandidate.getName())
                    .withMode(WriteMode.ADD)
                    .withClientModified(new Date(uploadCandidate.lastModified()))
                    .uploadAndFinish(in);
            LOG.info("Upload to Dropbox was successful with file " + metadata.getName());
            SharedLinkMetadata slm = client.sharing().createSharedLinkWithSettings(metadata.getPathLower(), SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build());
            String url = slm.getUrl();

            eventService.publish(new UploadEvent(uploadCandidate.getName(), url));

        } catch (Exception e) {
            LOG.error("Upload to Dropbox FAILED!", e);
        }
    }
}
