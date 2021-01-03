package com.rudyii.hsw.services;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.providers.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rudyii.hsw.configuration.OptionsService.KEEP_DAYS;

@Slf4j
@Service
public class HouseKeepingService {
    private final StorageProvider storageProvider;
    private final IspService ispService;
    private final Connection connection;
    private final OptionsService optionsService;
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;

    @Value("#{hswProperties['video.archive.location']}")
    private String archiveLocation;

    @Autowired
    public HouseKeepingService(StorageProvider storageProvider, IspService ispService,
                               Connection connection, OptionsService optionsService,
                               FirebaseDatabaseProvider firebaseDatabaseProvider) {
        this.storageProvider = storageProvider;
        this.ispService = ispService;
        this.connection = connection;
        this.optionsService = optionsService;
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() {
        if (ispService.internetIsAvailable()) {
            AtomicInteger eventsDeleted = new AtomicInteger();
            AtomicInteger localFilesDeleted = new AtomicInteger();
            AtomicInteger remoteFilesDeleted = new AtomicInteger();
            long timeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(((Long) optionsService.getOption(KEEP_DAYS)));
            File localStorage = new File(archiveLocation);
            String selectQuery = "SELECT FILE_ID, CREATED from RECORD_FILES where CREATED < " + timeAgo;

            ResultSet resultSet;
            try {
                String fileName;
                resultSet = connection.prepareStatement(selectQuery).executeQuery();
                while (resultSet.next()) {
                    String rowToDeleteQuery;
                    fileName = resultSet.getString(1);
                    try {
                        File removeCandidate = new File(localStorage.getCanonicalPath() + "/" + fileName);
                        if (removeCandidate.delete()) {
                            localFilesDeleted.incrementAndGet();
                        } else {
                            log.info("Local file {} not found", removeCandidate);
                        }
                    } catch (IOException e) {
                        log.error("Failed to delete file", e);
                    }
                    if (fileName.endsWith(".event")) {
                        firebaseDatabaseProvider.getReference("/log/" + fileName.replaceAll(".event", "")).removeValueAsync();
                        eventsDeleted.incrementAndGet();
                    } else {
                        storageProvider.deleteData(fileName);
                        remoteFilesDeleted.incrementAndGet();
                    }
                    rowToDeleteQuery = "DELETE from RECORD_FILES WHERE FILE_ID = " + "'" + fileName + "'";
                    connection.createStatement().execute(rowToDeleteQuery);
                }
                resultSet.close();
            } catch (SQLException e) {
                log.error("Failed to get files list", e);
            }

            log.info("Totally deleted: local files: {}, remote files: {}, events: {}", localFilesDeleted.get(), remoteFilesDeleted.get(), eventsDeleted.get());
        }
    }
}
