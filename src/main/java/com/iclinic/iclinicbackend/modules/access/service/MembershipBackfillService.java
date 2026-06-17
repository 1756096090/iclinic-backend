package com.iclinic.iclinicbackend.modules.access.service;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Fase B de la migración multitenant: rellena {@code company_memberships} a partir de las
 * columnas legacy {@code users.company_id / branch_id / role}, sin tocar todavía la fuente
 * de verdad (doble escritura llega en Fase C/D).
 *
 * <p>Es <b>idempotente</b>: si ya existe la membresía (user, company) no la duplica, por lo
 * que puede ejecutarse en cada arranque o invocarse manualmente sin riesgo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MembershipBackfillService {

    private final UserRepository userRepository;
    private final CompanyMembershipRepository membershipRepository;

    /**
     * @return resumen de lo procesado.
     */
    @Transactional
    public BackfillResult backfill() {
        int membershipsCreated = 0;
        int membershipsSkipped = 0;
        int platformAdminsMarked = 0;

        for (User user : userRepository.findAll()) {
            // SUPER_ADMIN global → no entra en memberships; se marca como admin de plataforma.
            if (user.getRole() == UserRole.SUPER_ADMIN) {
                if (!Boolean.TRUE.equals(user.getIsPlatformAdmin())) {
                    user.setIsPlatformAdmin(true);
                    userRepository.save(user);
                    platformAdminsMarked++;
                }
                continue;
            }

            // Usuarios sin empresa (p. ej. pacientes temporales de Firebase) no generan membresía.
            if (user.getCompany() == null) {
                continue;
            }

            Long companyId = user.getCompany().getId();
            if (membershipRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
                membershipsSkipped++;
                continue;
            }

            CompanyMembership membership = CompanyMembership.builder()
                    .user(user)
                    .company(user.getCompany())
                    .role(user.getRole())
                    // Heurística de migración: el ADMIN existente pasa a OWNER de su empresa.
                    .isOwner(user.getRole() == UserRole.ADMIN)
                    .active(Boolean.TRUE.equals(user.getActive()))
                    .branches(new HashSet<>())
                    .build();

            // Copia la sucursal legacy al acceso N:N (vacío = todas las de la empresa).
            if (user.getBranch() != null) {
                membership.getBranches().add(user.getBranch());
            }

            membershipRepository.save(membership);
            membershipsCreated++;
        }

        BackfillResult result = new BackfillResult(membershipsCreated, membershipsSkipped, platformAdminsMarked);
        log.info("Membership backfill done: {}", result);
        return result;
    }

    public record BackfillResult(int membershipsCreated, int membershipsSkipped, int platformAdminsMarked) {
    }
}
