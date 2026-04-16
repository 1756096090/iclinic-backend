package com.iclinic.iclinicbackend.modules.crm.message.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageNotificationDto {
    private Long messageId;
    private Long conversationId;
    private Long contactId;
    private String contactName;
    private ChannelType channelType;
    private String preview;
    private Instant receivedAt;
    private boolean newConversation;
}
