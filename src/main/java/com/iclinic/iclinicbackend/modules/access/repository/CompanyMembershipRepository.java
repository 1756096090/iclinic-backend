package com.iclinic.iclinicbackend.modules.access.repository;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, Long> {

    List<CompanyMembership> findByUserIdAndActiveTrue(Long userId);

    List<CompanyMembership> findByCompanyIdAndActiveTrue(Long companyId);

    Optional<CompanyMembership> findByUserIdAndCompanyId(Long userId, Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    long countByCompanyIdAndIsOwnerTrueAndActiveTrue(Long companyId);

    List<CompanyMembership> findByCompanyIdAndRoleAndActiveTrue(Long companyId, UserRole role);
}
