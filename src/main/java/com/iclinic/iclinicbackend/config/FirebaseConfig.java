package com.iclinic.iclinicbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account.path}")
    private String serviceAccountPath;

    /**
     * Conditionally creates FirebaseAuth bean only if the service account file exists.
     * For development without Firebase credentials, this bean won't be created.
     */
    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        File serviceAccountFile = new File(serviceAccountPath);

        if (!serviceAccountFile.exists()) {
            log.warn("Firebase service account file not found at: {}", serviceAccountPath);
            log.warn("Firebase authentication will NOT be available. For development, this is OK.");
            log.warn("To enable Firebase, create the file or set FIREBASE_SERVICE_ACCOUNT environment variable.");
            return null;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            try {
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(new FileInputStream(serviceAccountPath));
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
                throw e;
            }
        }
        return FirebaseAuth.getInstance();
    }
}
