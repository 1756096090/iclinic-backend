package com.iclinic.iclinicbackend.modules.access.repository;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, Long> {

    List<CompanyMembership> findByUserIdAndActiveTrue(Long userId);

    List<CompanyMembership> findByCompanyIdAndActiveTrue(Long companyId);

    Optional<CompanyMembership> findByUserIdAndCompanyId(Long userId, Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    long countByCompanyIdAndIsOwnerTrueAndActiveTrue(Long companyId);

    List<CompanyMembership> findByCompanyIdAndRoleAndActiveTrue(Long companyId, UserRole role);

    /**
     * Membresias activas con la empresa y las sucursales YA CARGADAS.
     * <p>
     * El {@code LEFT JOIN FETCH} sobre las sucursales no es una optimizacion
     * suelta: {@code AmbitoDeUsuario} responde 19 preguntas por peticion y varias
     * miran las sucursales. Sin el, cada una dispararia su propia consulta sobre
     * una coleccion perezosa — el N+1 clasico, en el camino de autorizacion.
     * <p>
     * {@code DISTINCT} porque el join con la coleccion multiplica las filas.
     */
    @Query("SELECT DISTINCT m FROM CompanyMembership m "
         + "JOIN FETCH m.company "
         + "LEFT JOIN FETCH m.branches "
         + "WHERE m.user.id = :userId AND m.active = true")
    List<CompanyMembership> findActivasConEmpresaYSucursales(@Param("userId") Long userId);
}
