package com.iclinic.iclinicbackend.modules.crm.mapper;

import com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationResponseDto toResponseDto(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        return ConversationResponseDto.builder()
                .id(conversation.getId())
                .contactId(conversation.getContact() != null ? conversation.getContact().getId() : null)
                .channelConnectionId(conversation.getChannelConnection() != null ? conversation.getChannelConnection().getId() : null)
                .assignedUserId(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getId() : null)
                .status(conversation.getStatus())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}

