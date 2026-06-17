package com.iclinic.iclinicbackend.modules.access.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bitácora de acciones sensibles (creación/borrado de usuarios, cambios de rol,
 * accesos cross-tenant denegados, onboarding, etc.). Imprescindible en SaaS
 * multitenant para trazabilidad y respuesta a incidentes.
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_company", columnList = "company_id"),
                @Index(name = "idx_audit_actor", columnList = "actor_user_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que ejecuta la acción (null si es del sistema). */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /** Empresa sobre la que se actúa (null para acciones de plataforma). */
    @Column(name = "company_id")
    private Long companyId;

    /** Ej: "USER_CREATE", "ACCESS_DENIED", "ROLE_CHANGED". */
    @Column(nullable = false, length = 80)
    private String action;

    /** Tipo de entidad afectada. Ej: "User", "Appointment". */
    @Column(length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** Detalle libre (JSON serializado, IP, valores antes/después). */
    @Column(length = 2000)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
