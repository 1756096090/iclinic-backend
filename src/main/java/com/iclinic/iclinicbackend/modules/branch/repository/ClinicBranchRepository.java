package com.iclinic.iclinicbackend.modules.branch.repository;

import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicBranchRepository extends JpaRepository<ClinicBranch, Long> {
}