package com.rudyii.hsw.services.system;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.logs.LogBase;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.providers.StorageProvider;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import com.rudyii.hsw.services.internet.IspService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.*;

@Slf4j
@Service
public class HouseKeepingService {
    private final StorageProvider storageProvider;
    private final IspService ispService;
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final List<String> cameraNames;

    public HouseKeepingService(StorageProvider storageProvider, IspService ispService,
                               FirebaseDatabaseProvider firebaseDatabaseProvider, FirebaseGlobalSettingsService globalSettingsService,
                               List<Camera> cameras) {
        this.storageProvider = storageProvider;
        this.ispService = ispService;
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.globalSettingsService = globalSettingsService;
        this.cameraNames = cameras.stream().map(Camera::getCameraName).collect(Collectors.toList());
    }

    @PostConstruct
    public void cleanupUnconfiguredCamera() {
        firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataSnapshot.getChildren().forEach(cameraSettings -> {
                        String cameraName = cameraSettings.getKey();
                        if (!cameraNames.contains(cameraName)) {
                            firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA).child(cameraName).removeValueAsync();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() {
        if (ispService.internetIsAvailable()) {
            AtomicInteger eventsDeleted = new AtomicInteger();
            AtomicInteger remoteFilesDeleted = new AtomicInteger();
            long timeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis((globalSettingsService.getGlobalSettings().getHistoryDays()));

            storageProvider.getAllBlobs().forEach(blob -> {
                if (blob.getCreateTime() < timeAgo) {
                    blob.delete();
                    remoteFilesDeleted.incrementAndGet();
                }
            });

            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<Long> logsToRemove = new ArrayList<>();
                        dataSnapshot.getChildren().forEach(logRecord -> {
                            LogBase logBase = logRecord.getValue(LogBase.class);
                            if (logBase.getEventId() < timeAgo) {
                                logsToRemove.add(logBase.getEventId());
                            }
                        });

                        logsToRemove.forEach(timestamp -> {
                            firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(timestamp)).removeValueAsync();
                            eventsDeleted.incrementAndGet();
                        });
                    } else {
                        log.info("Log is empty, nothing to remove");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            log.info("Totally deleted: remote files: {}, events: {}", remoteFilesDeleted.get(), eventsDeleted.get());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupObsolete() {
        firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).addListenerForSingleValueEvent(getUsageStatsCleanupValueEventListener());
    }

    private ValueEventListener getUsageStatsCleanupValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> datesToRemove = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    dataSnapshot.getChildren().forEach(dataSnapshotPerDay -> {
                        if (beforeHistoricalToday(dataSnapshotPerDay.getKey())) {
                            datesToRemove.add(dataSnapshotPerDay.getKey());
                        }
                    });
                }
                datesToRemove.forEach(date -> firebaseDatabaseProvider.getRootReference().child(USAGE_STATS_ROOT).child(date).removeValueAsync());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Weekly stats Firebase data!");
            }
        };
    }

    private boolean beforeHistoricalToday(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate aDate = LocalDate.parse(date, formatter);
        return aDate.isBefore(getHistoricalToday());
    }

    private LocalDate getHistoricalToday() {
        return LocalDate.now().minusDays(globalSettingsService.getGlobalSettings().getHistoryDays());
    }
}
