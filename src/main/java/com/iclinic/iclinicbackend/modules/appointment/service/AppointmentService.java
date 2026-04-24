package com.iclinic.iclinicbackend.modules.appointment.service;

import com.iclinic.iclinicbackend.modules.appointment.dto.*;
import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<AvailableSlotDto> getAvailableSlots(Long branchId, LocalDate date);

    AppointmentResponseDto createAppointment(CreateAppointmentRequestDto dto);

    AppointmentResponseDto rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDto dto);

    AppointmentResponseDto cancelAppointment(Long appointmentId, CancelAppointmentRequestDto dto);

    List<AppointmentResponseDto> findByBranch(Long branchId);

    List<AppointmentResponseDto> findByContact(Long contactId);

    Appointment findEntityById(Long appointmentId);
}

