package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.BranchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, Long> {

    List<BranchSchedule> findByDoctorIdAndActiveTrueOrderByDayOfWeekAsc(Long doctorId);

    Optional<BranchSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
}