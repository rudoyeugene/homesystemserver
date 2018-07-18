package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.objects.events.ServerKeyUpdatedEvent;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
public class ClientsService {
    private static Logger LOG = LogManager.getLogger(ClientsService.class);

    private List<Client> clients;
    private DatabaseReference connectedClientsRef;
    private ValueEventListener connectedClientsValueEventListener;

    @Autowired
    public ClientsService(FirebaseDatabaseProvider firebaseDatabaseProvider){
        this.clients = Collections.synchronizedList(new ArrayList());
        connectedClientsRef = firebaseDatabaseProvider.getReference("/connectedClients");
        connectedClientsValueEventListener = getConnectedClientsValueEventListener();
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    @PostConstruct
    public void initService(){
        connectedClientsRef.removeEventListener(connectedClientsValueEventListener);
        connectedClientsRef.addValueEventListener(connectedClientsValueEventListener);
    }

    private ValueEventListener getConnectedClientsValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> connectedClients = (HashMap<String, Object>) dataSnapshot.getValue();

                if (connectedClients == null) {
                    LOG.warn("No connected clients found!");
                } else {
                    clients.clear();

                    connectedClients.forEach((userId, value) -> {
                        HashMap<String, Object> userProperties = (HashMap<String, Object>) value;
                        clients.add(new Client(userId, userProperties));
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Connected Clients Firebase data!");
            }
        };
    }

    public List<Client> getClients() {
        return clients;
    }
}
