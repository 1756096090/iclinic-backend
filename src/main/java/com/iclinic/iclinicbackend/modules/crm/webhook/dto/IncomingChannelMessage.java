package com.iclinic.iclinicbackend.modules.crm.webhook.dto;

import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingChannelMessage {

    private Long companyId;
    private ChannelType channelType;
    private String externalUserId;
    private String externalChatId;
    private String externalMessageId;
    private String displayName;
    private String username;
    private String text;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /** Teléfono normalizado (+E.164). Null para Telegram; presente para WhatsApp. */
    private String phone;

    private Instant timestamp;

    public void validate() {
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (channelType == null) throw new IllegalArgumentException("channelType is required");
        if (externalUserId == null || externalUserId.isBlank()) throw new IllegalArgumentException("externalUserId is required");
        if (externalChatId == null || externalChatId.isBlank()) throw new IllegalArgumentException("externalChatId is required");
        if (externalMessageId == null || externalMessageId.isBlank()) throw new IllegalArgumentException("externalMessageId is required");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
    }
}
