package com.iclinic.iclinicbackend.modules.crm.conversation.entity;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.entity.BaseEntity;
import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "crm_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private CrmContact contact;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_connection_id")
    private ChannelConnection channelConnection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status;

    @Column(nullable = false)
    private Instant lastMessageAt;

    @PrePersist
    public void prePersist() {
        super.onCreate();
        if (this.lastMessageAt == null) this.lastMessageAt = Instant.now();
        if (this.status == null) this.status = ConversationStatus.OPEN;
    }

    @PreUpdate
    public void preUpdate() {
        super.onUpdate();
    }
}