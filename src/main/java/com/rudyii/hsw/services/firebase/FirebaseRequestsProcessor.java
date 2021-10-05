package com.rudyii.hsw.services.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.ServerStatusChangeRequest;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.services.SystemModeAndStateService;
import com.rudyii.hsw.services.messaging.ReportingService;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.*;
import static com.rudyii.hs.common.names.StaticData.BY_SYSTEM;
import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.isHomeSystemInitComplete;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseRequestsProcessor {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final SystemModeAndStateService systemModeAndStateService;
    private final EventService eventService;
    private final ReportingService reportingService;
    private final ArrayList<DatabaseReference> databaseReferences = new ArrayList<>();
    private final ArrayList<ValueEventListener> valueEventListeners = new ArrayList<>();

    @PostConstruct
    public void init() {
        firebaseDatabaseProvider.getRootReference().child(REQUEST_ROOT).child(REQUEST_HOURLY_REPORT).setValueAsync(0);
        firebaseDatabaseProvider.getRootReference().child(REQUEST_ROOT).child(REQUEST_SYSTEM_MODE_AND_STATE).setValueAsync(ServerStatusChangeRequest.builder()
                .systemMode(systemModeAndStateService.getSystemMode())
                .systemState(systemModeAndStateService.getSystemState())
                .by(BY_SYSTEM)
                .build());
        registerListeners();
    }

    @PreDestroy
    private void unregisterListeners() {
        for (DatabaseReference reference : databaseReferences) {
            for (ValueEventListener eventListener : valueEventListeners) {
                reference.removeEventListener(eventListener);
            }
        }
    }

    private void registerListeners() {
        DatabaseReference armRef = firebaseDatabaseProvider.getRootReference().child(REQUEST_ROOT).child(REQUEST_SYSTEM_MODE_AND_STATE);
        databaseReferences.add(armRef);
        ValueEventListener armRefValueEventListener = getArmedEventValueEventListener();
        valueEventListeners.add(armRefValueEventListener);
        armRef.addValueEventListener(armRefValueEventListener);

        DatabaseReference resendHourlyRef = firebaseDatabaseProvider.getRootReference().child(REQUEST_ROOT).child(REQUEST_HOURLY_REPORT);
        databaseReferences.add(resendHourlyRef);
        ValueEventListener resendHourlyRefValueEventListener = getResendHourlyValueEventListener();
        valueEventListeners.add(resendHourlyRefValueEventListener);
        resendHourlyRef.addValueEventListener(resendHourlyRefValueEventListener);
    }


    private ValueEventListener getResendHourlyValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isHomeSystemInitComplete() && dataSnapshot.exists()) {
                    reportingService.sendHourlyReport();
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Hourly Resend Firebase data!");
            }
        };
    }

    private ValueEventListener getArmedEventValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isHomeSystemInitComplete() && dataSnapshot.exists()) {
                    ServerStatusChangeRequest serverStatusChangeRequest = dataSnapshot.getValue(ServerStatusChangeRequest.class);
                    eventService.publish(ArmedEvent.builder()
                            .systemMode(serverStatusChangeRequest.getSystemMode())
                            .systemState(serverStatusChangeRequest.getSystemState())
                            .by(serverStatusChangeRequest.getBy())
                            .skipMe(true)
                            .build());
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Armed Mode & Armed State Firebase data!");
            }
        };
    }
}
