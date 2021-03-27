package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.providers.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rudyii.hsw.configuration.OptionsService.KEEP_DAYS;

@Slf4j
@Service
public class HouseKeepingService {
    private final StorageProvider storageProvider;
    private final IspService ispService;
    private final OptionsService optionsService;
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;

    @Autowired
    public HouseKeepingService(StorageProvider storageProvider, IspService ispService,
                               OptionsService optionsService, FirebaseDatabaseProvider firebaseDatabaseProvider) {
        this.storageProvider = storageProvider;
        this.ispService = ispService;
        this.optionsService = optionsService;
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void houseKeep() {
        if (ispService.internetIsAvailable()) {
            AtomicInteger eventsDeleted = new AtomicInteger();
            AtomicInteger remoteFilesDeleted = new AtomicInteger();
            long timeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(((Long) optionsService.getOption(KEEP_DAYS)));

            storageProvider.getAllBlobs().forEach(blob -> {
                if (blob.getCreateTime() < timeAgo) {
                    blob.delete();
                    remoteFilesDeleted.incrementAndGet();
                }
            });

            firebaseDatabaseProvider.getReference("/log").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        HashMap<String, Object> logs = new HashMap<>(((HashMap<String, Object>) dataSnapshot.getValue()));
                        logs.keySet().forEach(eventId -> {
                            if (Long.parseLong(eventId) < timeAgo) {
                                firebaseDatabaseProvider.getReference("/log/" + eventId).removeValueAsync();
                                eventsDeleted.incrementAndGet();
                            }
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
}
