package com.rudyii.hsw.services.firebase;

import com.rudyii.hs.common.objects.info.ServerStatus;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.SystemStateChangedEvent;
import com.rudyii.hsw.services.ArmedStateService;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.STATUS_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.STATUS_SERVER;

@Service
@AllArgsConstructor
public class FirebaseStatusProcessor {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final ArmedStateService armedStateService;

    @PostConstruct
    public void init() {
        firebaseDatabaseProvider.getRootReference().child(STATUS_ROOT).child(STATUS_SERVER).setValueAsync(ServerStatus.builder()
                .systemMode(armedStateService.getSystemMode())
                .systemState(armedStateService.getSystemState())
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @EventListener(SystemStateChangedEvent.class)
    public void updateStatus(SystemStateChangedEvent armedEvent) {
        firebaseDatabaseProvider.getRootReference().child(STATUS_ROOT).child(STATUS_SERVER).setValueAsync(ServerStatus.builder()
                .systemMode(armedEvent.getSystemMode())
                .systemState(armedEvent.getSystemState())
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
