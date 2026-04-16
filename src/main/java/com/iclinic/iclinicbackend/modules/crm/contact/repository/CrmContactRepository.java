package com.iclinic.iclinicbackend.modules.crm.contact.repository;

import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmContactRepository extends JpaRepository<CrmContact, Long> {

    /**
     * Búsqueda por teléfono principal heredado (campo phone del contacto).
     * Para lógica nueva preferir {@link CrmContactPhoneRepository}.
     */
    Optional<CrmContact> findByCompanyIdAndPhone(Long companyId, String phone);
}
