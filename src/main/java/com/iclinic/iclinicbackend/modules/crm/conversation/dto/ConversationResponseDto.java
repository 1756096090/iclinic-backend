package com.iclinic.iclinicbackend.modules.crm.conversation.dto;

import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConversationResponseDto {
    private Long id;
    private Long contactId;
    private Long channelConnectionId;
    private Long assignedUserId;
    private ConversationStatus status;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;
}
