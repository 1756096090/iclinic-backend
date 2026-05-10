package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("unused")
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBranchIdOrderByScheduledStartAsc(Long branchId);

    List<Appointment> findByDoctorIdOrderByScheduledStartAsc(Long doctorId);

    List<Appointment> findByDoctorIdAndScheduledStartBetweenOrderByScheduledStartAsc(
            Long doctorId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Appointment> findByContactIdOrderByScheduledStartDesc(Long contactId);

    List<Appointment> findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
            Long doctorId,
            List<AppointmentStatus> statuses,
            LocalDateTime end,
            LocalDateTime start
    );

    List<Appointment> findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThanAndIdNot(
            Long doctorId,
            List<AppointmentStatus> statuses,
            LocalDateTime end,
            LocalDateTime start,
            Long appointmentId
    );
}