package com.rudyii.hsw.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rudyii.hsw.services.system.ServerKeyService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FirebaseDatabaseProvider {
    private final ServerKeyService serverKeyService;
    private final FirebaseDatabase firebaseDatabase;

    public FirebaseDatabaseProvider(ServerKeyService serverKeyService) throws IOException {
        this.serverKeyService = serverKeyService;

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(this.getClass().getResourceAsStream("/server-global.json")))
                .setDatabaseUrl("https://complete-home-system.firebaseio.com/")
                .setDatabaseAuthVariableOverride(null)
                .build();

        FirebaseApp.initializeApp(options);

        this.firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public DatabaseReference getRootReference() {
        return firebaseDatabase.getReference(serverKeyService.getServerKey().toString());
    }
}
