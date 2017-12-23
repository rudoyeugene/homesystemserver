package com.rudyii.hsw.database;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FirebaseDatabaseProvider {

    public FirebaseDatabaseProvider() throws IOException {
        InputStream serviceAccount = this.getClass().getResourceAsStream("/server-global.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                .setDatabaseUrl("https://complete-home-system.firebaseio.com/")
                .setDatabaseAuthVariableOverride(null)
                .build();

        FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseDatabase getFirebaseDatabase(){
        return FirebaseDatabase.getInstance();
    }
}
