package com.rudyii.hsw.services;

import com.dropbox.core.v2.DbxClientV2;
import com.rudyii.hsw.configuration.OptionsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
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

import static com.rudyii.hsw.configuration.OptionsService.KEEP_DAYS;

@Slf4j
@Service
public class HouseKeepingService {
    private DbxClientV2 client;
    private IspService ispService;
    private Connection connection;
    private OptionsService optionsService;

    @Value("#{hswProperties['video.archive.location']}")
    private String archiveLocation;

    @Autowired
    public HouseKeepingService(DbxClientV2 client, IspService ispService,
                               Connection connection, OptionsService optionsService) {
        this.client = client;
        this.ispService = ispService;
        this.connection = connection;
        this.optionsService = optionsService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() throws ParseException {
        if (ispService.internetIsAvailable()) {
            Date olderThan = DateUtils.addDays(new Date(), -((Long) optionsService.getOption(KEEP_DAYS)).intValue());
            File localStorage = new File(archiveLocation);

            ArrayList<String> filesToDelete = new ArrayList<>();

            ResultSet resultSet;
            try {
                String filename;
                Date fileUploadDate;
                resultSet = connection.createStatement().executeQuery("SELECT * from DROPBOX_FILES");
                while (resultSet.next()) {
                    filename = resultSet.getString(1);
                    fileUploadDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(resultSet.getString(2));

                    if (fileUploadDate.before(olderThan)) {
                        filesToDelete.add(filename);
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to get Dropbox files list", e);
            }
            final int[] deletedLocalFilesCount = {0};
            final int[] deletedRemoteFilesCount = {0};

            filesToDelete.forEach(fileName -> {
                try {
                    File removeCandidate = new File(localStorage.getCanonicalPath() + "/" + fileName);
                    removeCandidate.delete();
                    log.info("Local file removed as outdated: " + removeCandidate.getCanonicalPath());
                    deletedLocalFilesCount[0]++;
                } catch (IOException e) {
                    log.error("Failed to remove local file: " + fileName + " due to error:\n", e);
                }
            });

            filesToDelete.forEach(fileName -> {
                try {

                    client.files().delete("/" + fileName);
                    log.info("Remote file removed as outdated: " + fileName);
                    deletedRemoteFilesCount[0]++;
                } catch (Exception e) {
                    log.error("Failed to remove remote file: " + fileName + " due to error:\n", e);
                }

            });

            log.info("Totally deleted:\nLocal files: " + deletedLocalFilesCount[0] + "\nRemote files: " + deletedRemoteFilesCount[0]);

            filesToDelete.forEach(fileName -> {
                try {
                    connection.createStatement().executeUpdate(String.format("DELETE FROM DROPBOX_FILES WHERE FILE_NAME = %s", "'" + fileName + "'"));
                } catch (SQLException e) {
                    log.error("Failed to remove remote data: " + fileName + " from database due to error:\n", e);
                }
            });
        }
    }
}
