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
import java.util.UUID;

// Repositorio de usuarios
@SuppressWarnings("unused")
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    /**
     * Resuelve el sujeto del token con su empresa y sucursal YA CARGADAS.
     * <p>
     * El {@code JOIN FETCH} no es una optimizacion: el {@code User} que devuelve
     * esta consulta vive en los detalles de la autenticacion y se usa despues de
     * que la sesion se haya cerrado. Sin el, cualquier lectura de la relacion
     * —{@code user.getCompany().getName()} en {@code /api/v1/auth/me}, por
     * ejemplo— lanza {@code LazyInitializationException} y sale un 500.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company LEFT JOIN FETCH u.branch "
         + "WHERE u.keycloakUserId = :keycloakUserId")
    Optional<User> findByKeycloakUserId(@Param("keycloakUserId") UUID keycloakUserId);
    boolean existsByEmail(String email);
    boolean existsByKeycloakUserId(UUID keycloakUserId);
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

     // ═══════════════════════════════════════════════════════════
     // SUPER_ADMIN Queries
     // ═══════════════════════════════════════════════════════════

     @Query("""
             select u
             from User u
             where u.role = 'SUPER_ADMIN'
               and u.active = true
             """)
     List<User> findActiveSuperAdmins();

     @Query("""
             select count(u) > 0
             from User u
             where u.role = 'SUPER_ADMIN'
               and u.active = true
             """)
     boolean existsActiveSuperAdmin();

     @Query("""
             select u
             from User u
             where u.role = 'SUPER_ADMIN'
             order by u.createdAt asc
             """)
     List<User> findAllSuperAdmins();

    /**
     * El id de empresa sin cargar la entidad.
     * <p>
     * Lo usa el puente de {@code TenantContextFilter}, que trabaja con un
     * {@code User} ya desatachado: acceder a la relacion perezosa alli lanza
     * {@code LazyInitializationException} y acaba en un 500.
     */
    @Query("SELECT u.company.id FROM User u WHERE u.id = :userId")
    Optional<Long> findCompanyIdByUserId(@Param("userId") Long userId);
}
