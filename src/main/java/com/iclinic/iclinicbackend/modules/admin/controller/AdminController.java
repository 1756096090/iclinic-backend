package com.iclinic.iclinicbackend.modules.admin.controller;

import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingRequestDto;
import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingResponseDto;
import com.iclinic.iclinicbackend.modules.admin.dto.InviteUserRequestDto;
import com.iclinic.iclinicbackend.modules.admin.service.AdminService;
import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/client-onboarding")
    public ResponseEntity<ClientOnboardingResponseDto> createClient(
            @Valid @RequestBody ClientOnboardingRequestDto dto) {
        return ResponseEntity.ok(adminService.createClient(dto));
    }

    @PostMapping("/users/invite")
    public ResponseEntity<AuthUserResponseDto> inviteUser(
            @Valid @RequestBody InviteUserRequestDto dto) {
        return ResponseEntity.ok(adminService.inviteUser(dto));
    }
}
