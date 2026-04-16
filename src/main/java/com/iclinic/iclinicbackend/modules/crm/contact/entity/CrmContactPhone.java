package com.iclinic.iclinicbackend.modules.crm.contact.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Teléfono de un contacto CRM.
 * <p>
 * Permite que una misma persona ({@link CrmContact}) tenga múltiples números
 * (e.g. celular personal, teléfono de trabajo). La unicidad de número se
 * garantiza a nivel de empresa mediante {@code normalizedPhone + company_id}.
 * <p>
 * {@code normalizedPhone} debe almacenarse en formato E.164 (e.g. "+593987654321")
 * para facilitar comparaciones entre canales.
 */
@Entity
@Table(
    name = "crm_contact_phones",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_contact_phone_company",
        columnNames = {"normalized_phone", "company_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmContactPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private CrmContact contact;

    /**
     * Número tal como lo reportó el canal (puede tener espacios, guiones, etc.).
     * Solo para referencia; no usar para búsquedas.
     */
    @Column(nullable = false, length = 30)
    private String rawPhone;

    /**
     * Número en formato E.164 ("+CCXXXXXXXXX"). Se usa para búsquedas y deduplicación.
     * Debe almacenarse siempre en este formato al crear el registro.
     */
    @Column(name = "normalized_phone", nullable = false, length = 20)
    private String normalizedPhone;

    /**
     * company_id duplicado aquí para facilitar el índice único sin JOIN.
     * Debe mantenerse igual al contact.company.id.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

