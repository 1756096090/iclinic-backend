package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBranchIdOrderByScheduledStartAsc(Long branchId);

    List<Appointment> findByBranchIdAndScheduledStartBetweenOrderByScheduledStartAsc(
            Long branchId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Appointment> findByContactIdOrderByScheduledStartDesc(Long contactId);

    List<Appointment> findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
            Long branchId,
            List<AppointmentStatus> statuses,
            LocalDateTime end,
            LocalDateTime start
    );
}