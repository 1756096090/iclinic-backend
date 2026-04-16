package com.iclinic.iclinicbackend.modules.company.repository;

import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EcuadorianCompanyRepository extends JpaRepository<EcuadorianCompany, Long> {
    Optional<EcuadorianCompany> findByRuc(String ruc);
}