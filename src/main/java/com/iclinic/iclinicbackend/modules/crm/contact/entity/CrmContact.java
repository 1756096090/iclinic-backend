package com.iclinic.iclinicbackend.modules.crm.contact.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.shared.entity.ActivableEntity;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Persona real del CRM, independientemente del canal por el que escribe.
 * <p>
 * Una misma persona puede escribir por Telegram, WhatsApp, etc. — todas esas
 * identidades externas se vinculan a través de {@link com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink}.
 * Los teléfonos se gestionan en {@link CrmContactPhone} (relación 1:N) para
 * soportar personas con múltiples números.
 * <p>
 * Migración progresiva: el campo {@code phone} se mantiene nullable para
 * retrocompatibilidad mientras se completa la migración a {@link CrmContactPhone}.
 * La restricción única por (company_id, phone) se elimina — la unicidad se
 * gestiona a nivel de {@link CrmContactPhone#normalizedPhone}.
 */
@Entity
@Table(name = "crm_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CrmContact extends ActivableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, length = 150)
    private String fullName;

    /**
     * Teléfono principal heredado. Nullable — Telegram no proporciona teléfono.
     * Preferir {@link CrmContactPhone} para lógica nueva.
     */
    @Column(nullable = true, length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    /** Canal por el que se creó el contacto originalmente. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChannelType sourceChannel;

    /** Teléfonos adicionales de esta persona (0 o más). */
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CrmContactPhone> phones = new ArrayList<>();
}