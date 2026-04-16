package com.iclinic.iclinicbackend.modules.crm.mapper;

import com.iclinic.iclinicbackend.modules.crm.message.dto.MessageResponseDto;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponseDto toResponseDto(CrmMessage m) {
        if (m == null) {
            return null;
        }
        return MessageResponseDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .direction(m.getDirection())
                .messageType(m.getMessageType())
                .status(m.getStatus())
                .externalMessageId(m.getExternalMessageId())
                .content(m.getContent())
                .mediaUrl(m.getMediaUrl())
                .sentByUserId(m.getSentByUser() != null ? m.getSentByUser().getId() : null)
                .createdAt(m.getCreatedAt())
                .build();
    }
}

