package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.BranchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, Long> {

    List<BranchSchedule> findByBranchIdAndActiveTrueOrderByDayOfWeekAsc(Long branchId);

    Optional<BranchSchedule> findByBranchIdAndDayOfWeekAndActiveTrue(Long branchId, DayOfWeek dayOfWeek);
}