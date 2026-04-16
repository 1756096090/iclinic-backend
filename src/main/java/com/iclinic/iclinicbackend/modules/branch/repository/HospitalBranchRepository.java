package com.iclinic.iclinicbackend.modules.branch.repository;

import com.iclinic.iclinicbackend.modules.branch.entity.HospitalBranch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalBranchRepository extends JpaRepository<HospitalBranch, Long> {
}