package com.iclinic.iclinicbackend.modules.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleAppointmentRequestDto {

    @NotNull(message = "La nueva fecha/hora de inicio es requerida")
    @Future(message = "La nueva cita debe agendarse en el futuro")
    private LocalDateTime scheduledStart;

    @NotNull(message = "La nueva fecha/hora de fin es requerida")
    @Future(message = "La nueva cita debe agendarse en el futuro")
    private LocalDateTime scheduledEnd;

    private String notes;
}


