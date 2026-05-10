package com.iclinic.iclinicbackend.modules.user.repository;

import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Repositorio de usuarios
@SuppressWarnings("unused")
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByBranchIdAndRoleAndActiveTrueOrderByFirstNameAsc(Long branchId, UserRole role);
    List<User> findByCompanyId(Long companyId);
    List<User> findByBranchId(Long branchId);
    List<User> findByActive(Boolean active);

    @Query("""
            select u
            from User u
            where u.branch.id = :branchId
              and (
                    lower(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))) like lower(concat('%', :query, '%'))
                 or lower(u.firstName) like lower(concat('%', :query, '%'))
                 or lower(u.lastName) like lower(concat('%', :query, '%'))
                 or lower(u.email) like lower(concat('%', :query, '%'))
                 or lower(coalesce(u.phone, '')) like lower(concat('%', :query, '%'))
              )
            """)
    Page<User> searchByBranchIdAndText(@Param("branchId") Long branchId,
                                       @Param("query") String query,
                                       Pageable pageable);
}

