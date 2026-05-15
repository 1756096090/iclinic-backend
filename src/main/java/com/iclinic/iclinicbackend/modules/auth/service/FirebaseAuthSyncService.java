package com.iclinic.iclinicbackend.modules.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FirebaseAuthSyncService {
    private static final String FIREBASE_PROVIDER = "FIREBASE";

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    public AuthUserResponseDto sync(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName();
        String picture = decodedToken.getPicture();

        Optional<User> existingByAuthId = userRepository.findByExternalAuthId(uid);
        User user;

        if (existingByAuthId.isPresent()) {
            user = existingByAuthId.get();
        } else {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                user = existingByEmail.get();
                user.setExternalAuthId(uid);
                user.setAuthProvider(FIREBASE_PROVIDER);
            } else {
                user = createTemporaryUser(uid, email, name, picture);
            }
        }

        user.setPhotoUrl(picture);
        userRepository.save(user);

        return buildResponseDto(user);
    }

    private User createTemporaryUser(String uid, String email, String name, String picture) {
        String[] nameParts = parseFullName(name);
        return EcuadorianUser.builder()
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .email(email)
                .password(null)
                .externalAuthId(uid)
                .authProvider(FIREBASE_PROVIDER)
                .photoUrl(picture)
                .active(false)
                .build();
    }

    private String[] parseFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        return new String[]{
                parts[0],
                parts.length > 1 ? parts[1] : ""
        };
    }

    private AuthUserResponseDto buildResponseDto(User user) {
        return AuthUserResponseDto.builder()
                .id(user.getId())
                .externalAuthId(user.getExternalAuthId())
                .authProvider(user.getAuthProvider())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .role(user.getRole())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .active(user.getActive())
                .build();
    }
}
