package com.iclinic.iclinicbackend.modules.auth.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import com.iclinic.iclinicbackend.modules.auth.service.FirebaseAuthSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/firebase")
@RequiredArgsConstructor
public class FirebaseAuthController {
    private final FirebaseAuthSyncService firebaseAuthSyncService;

    @PostMapping("/sync")
    public ResponseEntity<AuthUserResponseDto> sync(@RequestHeader("Authorization") String authHeader)
            throws FirebaseAuthException {
        String token = extractToken(authHeader);
        AuthUserResponseDto response = firebaseAuthSyncService.sync(token);
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
