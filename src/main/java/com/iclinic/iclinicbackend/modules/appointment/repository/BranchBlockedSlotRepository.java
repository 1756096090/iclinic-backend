package com.iclinic.iclinicbackend.modules.appointment.repository;

import com.iclinic.iclinicbackend.modules.appointment.entity.BranchBlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BranchBlockedSlotRepository extends JpaRepository<BranchBlockedSlot, Long> {

    List<BranchBlockedSlot> findByBranchIdAndActiveTrue(Long branchId);

    List<BranchBlockedSlot> findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Long branchId,
            LocalDateTime end,
            LocalDateTime start
    );
}