package com.iclinic.iclinicbackend.modules.crm.conversation.service;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;

import java.util.List;

public interface ConversationService {
    Conversation findOrCreateOpenConversation(CrmContact contact, ChannelConnection channelConnection);
    boolean hasOpenConversation(CrmContact contact, ChannelConnection channelConnection);
    Conversation assign(Long conversationId, Long userId);
    List<Conversation> findByBranch(Long branchId);
    List<Conversation> findAll();
    List<Conversation> findByCompany(Long companyId);
    Conversation close(Long conversationId);
}

