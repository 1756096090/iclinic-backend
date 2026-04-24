package com.iclinic.iclinicbackend.modules.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleAppointmentRequestDto {

    @NotNull(message = "La nueva fecha/hora de inicio es requerida")
    @Future(message = "La nueva cita debe agendarse en el futuro")
    private LocalDateTime scheduledStart;

    @NotNull(message = "La nueva fecha/hora de fin es requerida")
    @Future(message = "La nueva cita debe agendarse en el futuro")
    private LocalDateTime scheduledEnd;

    private String notes;
}

