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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private Connection connection;

    @Value("${video.archive.location}")
    private String archiveLocation;

    @Value("${video.archive.keep.days}")
    private int keepDays;

    @Autowired
    public HouseKeepingService(DbxClientV2 client, IspService ispService, Connection connection) {
        this.client = client;
        this.ispService = ispService;
        this.connection = connection;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() throws ParseException {
        if (ispService.internetIsAvailable()) {
            Date olderThan = DateUtils.addDays(new Date(), -keepDays);
            File localStorage = new File(archiveLocation);

            ArrayList<File> localFilesToDelete = new ArrayList<>();
            ArrayList<String> remoteFilesToDelete = new ArrayList<>();

            localFilesToDelete.addAll(IteratorUtils.toList(FileUtils.iterateFiles(localStorage, new AgeFileFilter(olderThan), null)));

            ResultSet resultSet;
            try {
                String filename;
                Date fileUploadDate;
                resultSet = connection.createStatement().executeQuery("SELECT * from DROPBOX_FILES");
                while (resultSet.next()) {
                    filename = resultSet.getString(1);
                    fileUploadDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(resultSet.getString(2));

                    if (fileUploadDate.before(olderThan)) {
                        remoteFilesToDelete.add(filename);
                    }
                }
            } catch (SQLException e) {
                LOG.error("Failed to get Dropbox files list", e);
            }
            final int[] deletedLocalFilesCount = {0};
            final int[] deletedRemoteFilesCount = {0};

            localFilesToDelete.forEach(file -> {
                try {
                    file.delete();
                    LOG.info("Local file removed as outdated: " + file.getCanonicalPath());
                    deletedLocalFilesCount[0]++;
                } catch (IOException e) {
                    LOG.error("Failed to remove local file: " + file.getName() + " due to error:\n", e);
                }
            });

            remoteFilesToDelete.forEach(fileName -> {
                try {

                    client.files().delete("/" + fileName);
                    LOG.info("Remote file removed as outdated: " + fileName);
                    deletedRemoteFilesCount[0]++;
                } catch (Exception e) {
                    LOG.error("Failed to remove remote file: " + fileName + " due to error:\n", e);
                }

            });

            LOG.info("Totally deleted:\nLocal files: " + deletedLocalFilesCount[0] + "\nRemote files: " + deletedRemoteFilesCount[0]);
        }
    }
}
