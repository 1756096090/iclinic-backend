package com.iclinic.iclinicbackend.modules.access.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Permiso atómico del sistema RBAC (ej. {@code USER_CREATE}, {@code APPOINTMENT_READ}).
 * Los permisos son globales (catálogo); los roles los agrupan.
 */
@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Código estable usado en @PreAuthorize / checks. Ej: "APPOINTMENT_WRITE". */
    @Column(nullable = false, length = 100)
    private String code;

    @Column(length = 255)
    private String description;
}
