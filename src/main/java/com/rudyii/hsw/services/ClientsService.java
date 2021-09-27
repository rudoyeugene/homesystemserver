package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.ConnectedClient;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.CLIENTS_ROOT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientsService {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private List<ConnectedClient> clients;

    @PostConstruct
    public void initService() {
        clients = new ArrayList<>();
        DatabaseReference connectedClientsRef = firebaseDatabaseProvider.getRootReference().child(CLIENTS_ROOT);
        ValueEventListener connectedClientsValueEventListener = getConnectedClientsValueEventListener();
        connectedClientsRef.removeEventListener(connectedClientsValueEventListener);
        connectedClientsRef.addValueEventListener(connectedClientsValueEventListener);
    }

    private ValueEventListener getConnectedClientsValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    clients.clear();
                    dataSnapshot.getChildren().forEach(dataSnapshotConnectedClient -> {
                        clients.add(dataSnapshotConnectedClient.getValue(ConnectedClient.class));
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Connected Clients Firebase data!");
            }
        };
    }

    public List<ConnectedClient> getClients() {
        return clients;
    }
}
