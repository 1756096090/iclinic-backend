package com.iclinic.iclinicbackend.modules.crm.message.service;

import com.iclinic.iclinicbackend.modules.crm.message.dto.SendMessageRequestDto;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.InboundWebhookMessageDto;

import java.util.List;

public interface MessageService {
    CrmMessage sendText(SendMessageRequestDto dto);
    void processInboundMessage(InboundWebhookMessageDto dto);
    List<CrmMessage> findByConversation(Long conversationId);
    List<CrmMessage> findAll();
}

