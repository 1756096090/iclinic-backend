package com.iclinic.iclinicbackend.modules.access.repository;

import com.iclinic.iclinicbackend.modules.access.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Rol de sistema (companyId null) por código. */
    Optional<Role> findByCodeAndCompanyIdIsNull(String code);

    Optional<Role> findByCodeAndCompanyId(String code, Long companyId);
}
