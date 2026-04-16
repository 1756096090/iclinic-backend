package com.iclinic.iclinicbackend.modules.crm.message.dto;

import com.iclinic.iclinicbackend.shared.enums.MessageDirection;
import com.iclinic.iclinicbackend.shared.enums.MessageStatus;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageResponseDto {
    private Long id;
    private Long conversationId;
    private MessageDirection direction;
    private MessageType messageType;
    private MessageStatus status;
    private String externalMessageId;
    private String content;
    private String mediaUrl;
    private Long sentByUserId;
    private Instant createdAt;
}
