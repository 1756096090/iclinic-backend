package com.iclinic.iclinicbackend.modules.crm.message.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.MessageDirection;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NewMessageEvent {
    private Long messageId;
    private Long conversationId;
    private Long contactId;
    private Long companyId;
    private ChannelType channelType;
    private MessageDirection direction;
    private MessageType messageType;
    private String content;
    private Instant createdAt;
}
