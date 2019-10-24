package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.objects.events.ServerKeyUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class ClientsService {
    private List<Client> clients;
    private DatabaseReference connectedClientsRef;
    private ValueEventListener connectedClientsValueEventListener;

    @Autowired
    public ClientsService(FirebaseDatabaseProvider firebaseDatabaseProvider) {
        this.clients = Collections.synchronizedList(new ArrayList());
        connectedClientsRef = firebaseDatabaseProvider.getReference("/connectedClients");
        connectedClientsValueEventListener = getConnectedClientsValueEventListener();
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    @PostConstruct
    public void initService() {
        connectedClientsRef.removeEventListener(connectedClientsValueEventListener);
        connectedClientsRef.addValueEventListener(connectedClientsValueEventListener);
    }

    private ValueEventListener getConnectedClientsValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> connectedClients = (HashMap<String, Object>) dataSnapshot.getValue();

                if (connectedClients == null) {
                    log.warn("No connected clients found!");
                } else {
                    clients.clear();

                    connectedClients.forEach((userId, value) -> {
                        HashMap<String, Object> userProperties = (HashMap<String, Object>) value;
                        clients.add(Client.builder()
                                .hourlyReportMuted(Boolean.TRUE.equals(userProperties.get("hourlyReportMuted")))
                                .notificationsMuted(Boolean.TRUE.equals(userProperties.get("notificationsMuted")))
                                .email(String.valueOf(userProperties.get("email")))
                                .device(String.valueOf(userProperties.get("device")))
                                .appVersion(String.valueOf(userProperties.get("appVersion")))
                                .token(String.valueOf(userProperties.get("token")))
                                .notificationType(String.valueOf(userProperties.get("notificationType")))
                                .build()
                        );
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Connected Clients Firebase data!");
            }
        };
    }

    public List<Client> getClients() {
        return clients;
    }
}
