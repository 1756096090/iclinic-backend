package com.iclinic.iclinicbackend.modules.crm.message.repository;

import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmMessageRepository extends JpaRepository<CrmMessage, Long> {
    List<CrmMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    Optional<CrmMessage> findByChannelTypeAndExternalMessageId(ChannelType channelType, String externalMessageId);
}

