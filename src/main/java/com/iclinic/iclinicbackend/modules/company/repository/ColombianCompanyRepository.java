package com.iclinic.iclinicbackend.modules.company.repository;

import com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColombianCompanyRepository extends JpaRepository<ColombianCompany, Long> {
    Optional<ColombianCompany> findByNit(String nit);
}