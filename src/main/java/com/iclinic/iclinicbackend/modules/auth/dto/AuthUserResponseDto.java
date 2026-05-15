package com.iclinic.iclinicbackend.modules.auth.dto;

import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponseDto {
    private Long id;
    private String externalAuthId;
    private String authProvider;
    private String email;
    private String fullName;
    private String photoUrl;
    private UserRole role;
    private Long companyId;
    private Long branchId;
    private Boolean active;
}
