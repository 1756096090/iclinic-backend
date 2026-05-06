package com.iclinic.iclinicbackend.modules.appointment.controller;

import com.iclinic.iclinicbackend.modules.appointment.dto.*;
import com.iclinic.iclinicbackend.modules.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Gestión de citas médicas")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/available-slots")
    @Operation(
            summary = "Obtener slots disponibles",
            description = "Retorna los horarios disponibles para una sucursal en una fecha específica"
    )
    @ApiResponse(responseCode = "200", description = "Slots obtenidos exitosamente")
    @ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    public ResponseEntity<List<AvailableSlotDto>> getAvailableSlots(
            @Parameter(description = "ID de la sucursal", required = true)
            @RequestParam Long branchId,

            @Parameter(description = "Fecha (formato: yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(branchId, date));
    }

    @PostMapping
    @Operation(
            summary = "Crear nueva cita",
            description = "Crea una nueva cita médica con validaciones de disponibilidad"
    )
    @ApiResponse(responseCode = "201", description = "Cita creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o conflicto de horario")
    @ApiResponse(responseCode = "404", description = "Empresa, sucursal o contacto no encontrado")
    public ResponseEntity<AppointmentResponseDto> createAppointment(
            @Valid @RequestBody CreateAppointmentRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(dto));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(
            summary = "Reagendar cita",
            description = "Cambia la fecha y hora de una cita existente"
    )
    @ApiResponse(responseCode = "200", description = "Cita reagendada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o conflicto de horario")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
            @Parameter(description = "ID de la cita", required = true)
            @PathVariable Long id,

            @Valid @RequestBody RescheduleAppointmentRequestDto dto
    ) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, dto));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar cita",
            description = "Cancela una cita médica existente"
    )
    @ApiResponse(responseCode = "200", description = "Cita cancelada exitosamente")
    @ApiResponse(responseCode = "400", description = "Cita ya está cancelada")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(
            @Parameter(description = "ID de la cita", required = true)
            @PathVariable Long id,

            @Valid @RequestBody CancelAppointmentRequestDto dto
    ) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, dto));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(
            summary = "Obtener citas por sucursal",
            description = "Retorna todas las citas de una sucursal ordenadas por fecha"
    )
    @ApiResponse(responseCode = "200", description = "Citas obtenidas exitosamente")
    @ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    public ResponseEntity<List<AppointmentResponseDto>> findByBranch(
            @Parameter(description = "ID de la sucursal", required = true)
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(appointmentService.findByBranch(branchId));
    }

    @GetMapping("/contact/{contactId}")
    @Operation(
            summary = "Obtener citas por contacto",
            description = "Retorna todas las citas de un contacto ordenadas por fecha descendente"
    )
    @ApiResponse(responseCode = "200", description = "Citas obtenidas exitosamente")
    @ApiResponse(responseCode = "404", description = "Contacto no encontrado")
    public ResponseEntity<List<AppointmentResponseDto>> findByContact(
            @Parameter(description = "ID del contacto", required = true)
            @PathVariable Long contactId
    ) {
        return ResponseEntity.ok(appointmentService.findByContact(contactId));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener cita por ID",
            description = "Retorna los detalles de una cita específica"
    )
    @ApiResponse(responseCode = "200", description = "Cita obtenida exitosamente")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(
            @Parameter(description = "ID de la cita", required = true)
            @PathVariable Long id
    ) {
        var appointment = appointmentService.findEntityById(id);
        var appointmentMapper = new com.iclinic.iclinicbackend.modules.appointment.mapper.AppointmentMapper();
        return ResponseEntity.ok(appointmentMapper.toResponseDto(appointment));
    }
}

