package com.iclinic.iclinicbackend.modules.user.repository;

import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repositorio de usuarios
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByCompanyId(Long companyId);
    List<User> findByBranchId(Long branchId);
    List<User> findByActive(Boolean active);
}

