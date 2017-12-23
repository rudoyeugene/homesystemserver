package com.rudyii.hsw.services;

import com.dropbox.core.v2.DbxClientV2;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by jack on 16.02.17.
 */
@Service
public class HouseKeepingService {
    private static Logger LOG = LogManager.getLogger(HouseKeepingService.class);
    private DbxClientV2 client;
    private IspService ispService;

    @Value("${video.archive.location}")
    private String archiveLocation;

    @Value("${video.archive.keep.days}")
    private int keepDays;

    @Autowired
    public HouseKeepingService(DbxClientV2 client, IspService ispService) {
        this.client = client;
        this.ispService = ispService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() {
        if (ispService.internetIsAvailable()) {
            Date olderThan = DateUtils.addDays(new Date(), -keepDays);
            File localStorage = new File(archiveLocation);

            ArrayList<File> filesToDelete = new ArrayList<>();
            filesToDelete.addAll(IteratorUtils.toList(FileUtils.iterateFiles(localStorage, new AgeFileFilter(olderThan), null)));

            int localFilesToDelete = filesToDelete.size();
            final int[] remoteFilesToDelete = {0}; //Lambda workaround

            filesToDelete.forEach(file -> {
                try {
                    file.delete();
                    LOG.info("Local file removed as outdated: " + file.getCanonicalPath());
                } catch (IOException e) {
                    LOG.error("Failed to remove local file: " + file.getName() + " due to error:\n", e);
                }
                try {
                    client.files().delete("/" + file.getName());
                    LOG.info("Remote file removed as outdated: " + file.getName());
                    remoteFilesToDelete[0]++;
                } catch (Exception e) {
                    LOG.error("Failed to remove remote file: " + file.getName() + " due to error:\n", e);
                }
            });

            LOG.info("Totally deleted:\nLocal files: " + localFilesToDelete + "\nRemote files: " + remoteFilesToDelete[0]);
        }
    }
}
