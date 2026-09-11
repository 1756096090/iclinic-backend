package com.iclinic.iclinicbackend.shared.exception;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.iclinic.iclinicbackend.modules.auth.controller.FirebaseAuthController;
import com.iclinic.iclinicbackend.modules.auth.service.FirebaseAuthSyncService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FirebaseAuthExceptionHandlerTest {
    @ParameterizedTest
    @EnumSource(value = AuthErrorCode.class, names = {
            "EXPIRED_ID_TOKEN", "INVALID_ID_TOKEN", "REVOKED_ID_TOKEN", "USER_DISABLED"})
    void rejectedCredentialsReturn401(AuthErrorCode code) throws Exception {
        mvcFor(code).perform(post("/api/v1/auth/firebase/sync")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.details").value(code.name()))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/firebase/sync"));
    }

    @ParameterizedTest
    @EnumSource(value = AuthErrorCode.class, names = {"CERTIFICATE_FETCH_FAILED", "CONFIGURATION_NOT_FOUND"})
    void infrastructureErrorsRemainServerErrors(AuthErrorCode code) throws Exception {
        mvcFor(code).perform(post("/api/v1/auth/firebase/sync")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.details").value("FIREBASE_AUTH_ERROR"));
    }

    private MockMvc mvcFor(AuthErrorCode code) throws Exception {
        FirebaseAuthSyncService service = mock(FirebaseAuthSyncService.class);
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getAuthErrorCode()).thenReturn(code);
        when(service.sync("test-token")).thenThrow(exception);
        return MockMvcBuilders.standaloneSetup(new FirebaseAuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
