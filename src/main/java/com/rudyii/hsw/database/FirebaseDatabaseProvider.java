package com.rudyii.hsw.database;

import com.google.api.core.ApiFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rudyii.hsw.services.UuidService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class FirebaseDatabaseProvider {
    private UuidService uuidService;
    private FirebaseDatabase firebaseDatabase;

    public FirebaseDatabaseProvider(UuidService uuidService) throws IOException {
        this.uuidService = uuidService;

        InputStream serviceAccount = this.getClass().getResourceAsStream("/server-global.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
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
