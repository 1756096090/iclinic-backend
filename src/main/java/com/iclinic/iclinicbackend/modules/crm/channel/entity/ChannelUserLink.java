package com.iclinic.iclinicbackend.modules.crm.channel.entity;

import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "crm_channel_user_links",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"contact_id", "channel_type"}),
                @UniqueConstraint(columnNames = {"channel_type", "external_user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelUserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private CrmContact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channelType;

    @Column(nullable = false, length = 100)
    private String externalUserId;

    @Column(nullable = false, length = 100)
    private String externalChatId;

    @Column(length = 100)
    private String username;

    @Column(length = 150)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;


    /**
     * Empresa propietaria, denormalizada. NO es redundante: es la columna sobre
     * la que actua la politica de RLS y la que respalda la FK compuesta. Se
     * deriva del padre en {@code @PrePersist} para que ningun camino de alta
     * pueda dejarla incoherente.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @PrePersist
    public void prePersist() {
        if (this.companyId == null && contact != null) this.companyId = contact.getCompany().getId();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
