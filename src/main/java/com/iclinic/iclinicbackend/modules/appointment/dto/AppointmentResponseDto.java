package com.iclinic.iclinicbackend.modules.appointment.dto;

import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponseDto {
    private Long id;
    private Long companyId;
    private Long branchId;
    private Long contactId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private AppointmentStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

