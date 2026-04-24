package com.iclinic.iclinicbackend.modules.crm.channel.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_channel_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelProvider provider;

    @Column(length = 100)
    private String externalAccountId;

    @Column(length = 100)
    private String externalPhoneNumberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(length = 150)
    private String webhookVerifyToken;

    @Column(columnDefinition = "TEXT")
    private String webhookRegisteredUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelConnectionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ChannelConnectionStatus.PENDING;
        }
    }
}