package com.iclinic.iclinicbackend.modules.appointment.mapper;

import com.iclinic.iclinicbackend.modules.appointment.dto.AppointmentResponseDto;
import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponseDto toResponseDto(Appointment appointment) {
        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .companyId(appointment.getCompany().getId())
                .branchId(appointment.getBranch().getId())
                .contactId(appointment.getContact().getId())
                .doctorId(appointment.getDoctor() != null ? appointment.getDoctor().getId() : null)
                .doctorName(appointment.getDoctor() != null
                        ? appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName()
                        : null)
                .scheduledStart(appointment.getScheduledStart())
                .scheduledEnd(appointment.getScheduledEnd())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}

