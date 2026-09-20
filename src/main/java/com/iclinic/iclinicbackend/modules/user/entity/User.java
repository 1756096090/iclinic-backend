package com.iclinic.iclinicbackend.modules.user.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.SubjectType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    /**
     * OBSOLETA. La sustituye {@code company_membership_roles} en el bloque D.
     * El filtro de autenticacion ya no la lee: el rol efectivo es el del JWT
     * intersecado con el de la membresia. Se conserva mientras quede codigo de
     * negocio que la consulte, y se elimina con la migracion de roles.
     */
    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * SUPER_ADMIN global de plataforma (sin empresa). Modelo SaaS objetivo: la pertenencia
     * a empresas vive en {@code company_memberships}; este flag identifica al admin global.
     * Nullable de forma transitoria para no romper el seed legacy (null == false).
     */
    @Column
    @Builder.Default
    private Boolean isPlatformAdmin = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt;

    /** El {@code sub} del token de Keycloak. Fuente de verdad de la identidad. */
    @Column(name = "keycloak_user_id", unique = true)
    private UUID keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    @Builder.Default
    private SubjectType subjectType = SubjectType.HUMAN;

    /** {@code azp} del cliente. Obligatorio si y solo si es cuenta de servicio. */
    @Column(name = "keycloak_client_id", length = 120)
    private String keycloakClientId;

    /**
     * Interruptor de emergencia: se rechaza (401) todo token cuyo {@code iat} sea
     * anterior a esta marca. Un UPDATE corta todas las sesiones vivas del usuario
     * en la siguiente peticion, sin estado de sesion en el backend.
     */
    @Column(name = "tokens_valid_from", nullable = false)
    @Builder.Default
    private Instant tokensValidFrom = Instant.now();

    @Column
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    public abstract String getDocumentNumber();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
