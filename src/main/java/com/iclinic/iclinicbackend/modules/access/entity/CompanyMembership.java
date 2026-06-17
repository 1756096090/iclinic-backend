package com.iclinic.iclinicbackend.modules.access.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Vínculo N:N entre un usuario y una empresa, con su rol dentro de ESA empresa.
 *
 * <p>Es la pieza central del modelo multitenant SaaS: reemplaza a las FKs directas
 * {@code users.company_id} / {@code users.branch_id} / {@code users.role}. Un mismo
 * usuario puede tener varias membresías (una por empresa) con roles distintos.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Único por (user, company) — un usuario no puede tener dos membresías en la misma empresa.</li>
 *   <li>{@code isOwner=true} marca al dueño principal; cada empresa debe tener al menos uno.</li>
 *   <li>{@code branches} vacío = acceso a todas las sucursales de la empresa; con valores =
 *       acceso restringido a ese subconjunto (siempre de la misma empresa).</li>
 *   <li>SUPER_ADMIN global NO usa esta tabla (ver {@code User.isPlatformAdmin}).</li>
 * </ul>
 */
@Entity
@Table(
        name = "company_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_membership_user_company",
                columnNames = {"user_id", "company_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CompanyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Rol dentro de esta empresa. Reutiliza el enum existente para migración gradual. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Sucursales accesibles. Vacío = todas las de la empresa.
     * La integridad (branch.company == company) se valida en el servicio.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "membership_branches",
            joinColumns = @JoinColumn(name = "membership_id"),
            inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    @Builder.Default
    private Set<Branch> branches = new HashSet<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
