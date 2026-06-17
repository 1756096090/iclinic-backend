package com.iclinic.iclinicbackend.modules.auth.controller;

import com.iclinic.iclinicbackend.modules.auth.dto.CurrentUserResponseDto;
import com.iclinic.iclinicbackend.modules.auth.service.CurrentUserService;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticación")
public class AuthMeController {

    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    @Operation(
            summary = "Obtener información del usuario actual",
            description = "Retorna los datos del usuario autenticado, incluyendo su rol, empresa y accesos"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CurrentUserResponseDto> getCurrentUser() {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(buildResponse(user));
    }

    private CurrentUserResponseDto buildResponse(User user) {
        boolean isSuperAdmin = currentUserService.isSuperAdmin();
        
        return CurrentUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .active(user.getActive())
                .role(user.getRole())
                .isSuperAdmin(isSuperAdmin)
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

