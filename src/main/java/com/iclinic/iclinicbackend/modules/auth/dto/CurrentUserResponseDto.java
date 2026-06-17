package com.iclinic.iclinicbackend.modules.auth.dto;

import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private Boolean active;
    private UserRole role;
    private Boolean isSuperAdmin;
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
}

