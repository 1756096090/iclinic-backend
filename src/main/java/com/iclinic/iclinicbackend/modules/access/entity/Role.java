package com.iclinic.iclinicbackend.modules.access.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol RBAC = conjunto de permisos.
 *
 * <p>{@code companyId == null} → rol de sistema (plantilla global: OWNER, ADMIN, DENTIST…).
 * {@code companyId != null} → rol personalizado de una empresa concreta (SaaS avanzado).
 */
@Entity
@Table(name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_code_company", columnNames = {"code", "company_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** Null = rol de sistema; valor = rol propio de esa empresa. */
    @Column(name = "company_id")
    private Long companyId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
