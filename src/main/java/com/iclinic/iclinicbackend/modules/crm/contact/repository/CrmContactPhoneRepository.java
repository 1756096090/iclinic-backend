package com.iclinic.iclinicbackend.modules.crm.contact.repository;

import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContactPhone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmContactPhoneRepository extends JpaRepository<CrmContactPhone, Long> {

    /**
     * Busca un contacto por su teléfono normalizado dentro de una empresa.
     * Usado para deduplicar personas que escriben desde distintos canales.
     */
    Optional<CrmContactPhone> findByNormalizedPhoneAndCompanyId(String normalizedPhone, Long companyId);
}

