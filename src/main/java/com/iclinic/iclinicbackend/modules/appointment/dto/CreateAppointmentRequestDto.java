package com.iclinic.iclinicbackend.modules.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequestDto {

    @NotNull(message = "El ID de empresa es requerido")
    private Long companyId;

    @NotNull(message = "El ID de sucursal es requerido")
    private Long branchId;

    @NotNull(message = "El ID del contacto es requerido")
    private Long contactId;

    @NotNull(message = "La fecha/hora de inicio es requerida")
    @Future(message = "La cita debe agendarse en el futuro")
    private LocalDateTime scheduledStart;

    @NotNull(message = "La fecha/hora de fin es requerida")
    @Future(message = "La cita debe agendarse en el futuro")
    private LocalDateTime scheduledEnd;

    private String notes;
}

