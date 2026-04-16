package com.iclinic.iclinicbackend.modules.branch.repository;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio de sucursal con métodos genéricos
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByCompanyId(Long companyId);
}