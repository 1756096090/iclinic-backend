package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

@SuppressWarnings("unused")
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBranchIdOrderByScheduledStartAsc(Long branchId);

    List<Appointment> findByDoctorIdOrderByScheduledStartAsc(Long doctorId);

    List<Appointment> findByDoctorIdAndScheduledStartBetweenOrderByScheduledStartAsc(
            Long doctorId,
            Instant start,
            Instant end
    );

    List<Appointment> findByContactIdOrderByScheduledStartDesc(Long contactId);

    List<Appointment> findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
            Long doctorId,
            List<AppointmentStatus> statuses,
            Instant end,
            Instant start
    );

    List<Appointment> findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThanAndIdNot(
            Long doctorId,
            List<AppointmentStatus> statuses,
            Instant end,
            Instant start,
            Long appointmentId
    );
}