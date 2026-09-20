package com.iclinic.iclinicbackend.modules.access.service;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Alta de la pertenencia de una persona a una empresa.
 * <p>
 * Hasta ahora <em>ninguna ruta de producción creaba una
 * {@link CompanyMembership}</em>: solo lo hacía un backfill al arrancar. Un
 * usuario creado por la API no tenía pertenencia hasta el siguiente reinicio, y
 * funcionaba por accidente porque la autorización todavía miraba
 * {@code users.company_id}. Esta clase es lo que cierra ese hueco.
 * <p>
 * <b>La membresía es la tenencia.</b> Un usuario sin ella no pertenece a ninguna
 * clínica y, en cuanto se retire el puente de {@code TenantContextFilter}, no
 * verá nada. Por eso el alta es <strong>atómica</strong> con la del usuario: si
 * la membresía falla, el usuario tampoco debe quedar. {@code Propagation.MANDATORY}
 * lo hace explícito — este método exige que ya haya una transacción abierta y
 * falla si alguien lo llama suelto, en vez de abrir una propia y dejar al usuario
 * huérfano cuando algo reviente después.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final CompanyMembershipRepository membershipRepository;

    /**
     * Da de alta la pertenencia. Debe llamarse dentro de la misma transacción que
     * crea el usuario.
     *
     * @param branch sucursal a la que se le da acceso, o {@code null} para toda la
     *               empresa.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CompanyMembership conceder(User user, Company company, UserRole role, Branch branch) {
        if (company == null) {
            throw new IllegalArgumentException(
                    "No se puede crear una membresia sin empresa. Un administrador de "
                    + "plataforma no lleva membresia: se marca con is_platform_admin.");
        }

        return membershipRepository.findByUserIdAndCompanyId(user.getId(), company.getId())
                .orElseGet(() -> {
                    Set<Branch> sucursales = new HashSet<>();
                    if (branch != null) {
                        sucursales.add(branch);
                    }

                    CompanyMembership membresia = CompanyMembership.builder()
                            .user(user)
                            .company(company)
                            .role(role)
                            // El primero de una empresa es su propietario. En las
                            // altas posteriores esto es false: no se otorga la
                            // propiedad por invitar a alguien.
                            .isOwner(esLaPrimeraDeLaEmpresa(company.getId()))
                            .active(true)
                            .branches(sucursales)
                            .build();

                    CompanyMembership guardada = membershipRepository.save(membresia);
                    log.info("Membresia creada: userId={} companyId={} rol={} owner={}",
                            user.getId(), company.getId(), role, guardada.getIsOwner());
                    return guardada;
                });
    }

    private boolean esLaPrimeraDeLaEmpresa(Long companyId) {
        return membershipRepository.findByCompanyIdAndActiveTrue(companyId).isEmpty();
    }
}
