package com.rudyii.hsw.database;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rudyii.hsw.services.UuidService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class FirebaseDatabaseProvider {
    private UuidService uuidService;
    private FirebaseDatabase firebaseDatabase;

    public FirebaseDatabaseProvider(UuidService uuidService) throws IOException {
        this.uuidService = uuidService;

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(this.getClass().getResourceAsStream("/server-global.json")))
                .setDatabaseUrl("https://complete-home-system.firebaseio.com/")
                .setDatabaseAuthVariableOverride(null)
                .build();

        FirebaseApp.initializeApp(options);

        this.firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public ApiFuture pushData(String path, Map<String, ?> value) {
        DatabaseReference reference = firebaseDatabase.getReference(uuidService.getServerKey() + path);
        return reference.setValueAsync(value);
    }

    public DatabaseReference getReference(String path) {
        return firebaseDatabase.getReference(uuidService.getServerKey() + path);
    }
}
