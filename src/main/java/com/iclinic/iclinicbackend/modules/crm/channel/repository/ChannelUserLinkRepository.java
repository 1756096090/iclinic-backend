package com.iclinic.iclinicbackend.modules.crm.channel.repository;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelUserLinkRepository extends JpaRepository<ChannelUserLink, Long> {
    Optional<ChannelUserLink> findByChannelTypeAndExternalUserId(ChannelType channelType, String externalUserId);
    Optional<ChannelUserLink> findByChannelTypeAndExternalChatId(ChannelType channelType, String externalChatId);
    Optional<ChannelUserLink> findByContactAndChannelType(CrmContact contact, ChannelType channelType);
}
