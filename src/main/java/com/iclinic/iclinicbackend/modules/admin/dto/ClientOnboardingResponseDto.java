package com.iclinic.iclinicbackend.modules.admin.dto;

import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOnboardingResponseDto {
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    private AuthUserResponseDto user;
    private String message;
}
