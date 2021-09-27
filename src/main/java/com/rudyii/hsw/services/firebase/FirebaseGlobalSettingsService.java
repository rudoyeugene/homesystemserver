package com.rudyii.hsw.services.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.settings.GlobalSettings;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.SettingsUpdatedEvent;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_GLOBAL;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseGlobalSettingsService {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final EventService eventService;
    private GlobalSettings globalSettings = GlobalSettings.builder()
            .showMotionArea(false)
            .hourlyReportEnabled(true)
            .hourlyReportForced(false)
            .monitoringEnabled(true)
            .verboseOutput(false)
            .gatherStats(true)
            .historyDays(7)
            .delayedArmTimeout(60)
            .build();

    @PostConstruct
    public void buildRefreshSettings() {
        firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_GLOBAL).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    GlobalSettings globalSettings = dataSnapshot.getValue(GlobalSettings.class);
                    setGlobalSettings(globalSettings);
                } else {
                    firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_GLOBAL).setValueAsync(globalSettings);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to update GlobalSettings ", databaseError);
            }
        });
    }

    public GlobalSettings getGlobalSettings() {
        return globalSettings;
    }

    private void setGlobalSettings(GlobalSettings globalSettings) {
        if (!this.globalSettings.equals(globalSettings)) {
            eventService.publish(SettingsUpdatedEvent.builder()
                    .globalSettings(globalSettings)
                    .build());
            this.globalSettings = globalSettings;
        }
    }
}
