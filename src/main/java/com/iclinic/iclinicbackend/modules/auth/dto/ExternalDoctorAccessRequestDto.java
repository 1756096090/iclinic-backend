package com.iclinic.iclinicbackend.modules.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDoctorAccessRequestDto {
    private Long externalDoctorId;
    private Long patientId;
    private Long companyId;
    private Long branchId;
    private Long appointmentId;
    private Long conversationId;
    private String reason;
    private LocalDateTime expiresAt;
}
