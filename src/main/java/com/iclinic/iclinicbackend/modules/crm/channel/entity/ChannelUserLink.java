package com.iclinic.iclinicbackend.modules.crm.channel.entity;

import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
