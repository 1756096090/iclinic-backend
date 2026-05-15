package com.iclinic.iclinicbackend.modules.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDoctorAccessResponseDto {
    private Long id;
    private Long externalDoctorId;
    private String externalDoctorName;
    private Long patientId;
    private Long companyId;
    private Long branchId;
    private Long appointmentId;
    private Long conversationId;
    private String reason;
    private Boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
