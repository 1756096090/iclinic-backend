package com.iclinic.iclinicbackend.modules.company.repository;

import com.iclinic.iclinicbackend.modules.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio de empresa con métodos genéricos
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByNameIgnoreCase(String name);
}