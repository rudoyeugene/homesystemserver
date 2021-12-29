package com.rudyii.hsw.services.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.settings.GlobalSettings;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
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
    private GlobalSettings globalSettings = GlobalSettings.builder().build();

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
                log.error("Failed to update GlobalSettings ", databaseError.toException());
            }
        });
    }

    public GlobalSettings getGlobalSettings() {
        return globalSettings;
    }

    private void setGlobalSettings(GlobalSettings globalSettings) {
        this.globalSettings = globalSettings;
    }
}
