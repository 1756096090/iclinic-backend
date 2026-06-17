package com.iclinic.iclinicbackend.modules.admin.controller;

import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingRequestDto;
import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingResponseDto;
import com.iclinic.iclinicbackend.modules.admin.dto.CreateSuperAdminRequestDto;
import com.iclinic.iclinicbackend.modules.admin.dto.InviteUserRequestDto;
import com.iclinic.iclinicbackend.modules.admin.service.AdminService;
import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Endpoints administrativos")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/client-onboarding")
    @Operation(summary = "Crear cliente (SUPER_ADMIN only)", description = "Crea una empresa completa con rama y usuario admin")
    public ResponseEntity<ClientOnboardingResponseDto> createClient(
            @Valid @RequestBody ClientOnboardingRequestDto dto) {
        return ResponseEntity.ok(adminService.createClient(dto));
    }

    @PostMapping("/users/invite")
    @Operation(summary = "Invitar usuario a la plataforma")
    public ResponseEntity<AuthUserResponseDto> inviteUser(
            @Valid @RequestBody InviteUserRequestDto dto) {
        return ResponseEntity.ok(adminService.inviteUser(dto));
    }

    // ═══════════════════════════════════════════════════════════
    // SUPER_ADMIN Management Endpoints
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/super-admins")
    @Operation(summary = "Crear nuevo SUPER_ADMIN (SUPER_ADMIN only)")
    public ResponseEntity<AuthUserResponseDto> createSuperAdmin(
            @Valid @RequestBody CreateSuperAdminRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.inviteUser(
                    InviteUserRequestDto.builder()
                            .firstName(dto.getFirstName())
                            .lastName(dto.getLastName())
                            .email(dto.getEmail())
                            .phone(dto.getPhone())
                            .role(com.iclinic.iclinicbackend.shared.enums.UserRole.SUPER_ADMIN)
                            .companyId(null) // SUPER_ADMIN no tiene company asignada
                            .userType(dto.getUserType())
                            .documentNumber(dto.getDocumentNumber())
                            .nationality(dto.getNationality())
                            .build()
                ));
    }

    @PatchMapping("/super-admins/{id}/deactivate")
    @Operation(summary = "Desactivar SUPER_ADMIN (SUPER_ADMIN only)")
    public ResponseEntity<AuthUserResponseDto> deactivateSuperAdmin(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateSuperAdmin(id));
    }
}
