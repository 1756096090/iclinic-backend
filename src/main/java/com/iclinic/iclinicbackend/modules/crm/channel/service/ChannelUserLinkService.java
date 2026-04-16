package com.iclinic.iclinicbackend.modules.crm.channel.service;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelUserLinkRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChannelUserLinkService {

    private final ChannelUserLinkRepository linkRepository;

    public ChannelUserLink findOrCreate(
            CrmContact contact,
            ChannelType channelType,
            String externalUserId,
            String externalChatId,
            String username,
            String displayName) {

        if (externalUserId == null || externalUserId.isBlank()) {
            throw new IllegalArgumentException("externalUserId cannot be empty");
        }
        if (externalChatId == null || externalChatId.isBlank()) {
            throw new IllegalArgumentException("externalChatId cannot be empty");
        }

        Optional<ChannelUserLink> existingLink = linkRepository
                .findByChannelTypeAndExternalUserId(channelType, externalUserId);

        if (existingLink.isPresent()) {
            ChannelUserLink link = existingLink.get();
            log.debug("ChannelUserLink already exists for channel={} externalUserId={}",
                    channelType, externalUserId);
            return link;
        }

        log.info("Creating new ChannelUserLink for contact={} channel={} externalUserId={}",
                contact.getId(), channelType, externalUserId);

        ChannelUserLink link = ChannelUserLink.builder()
                .contact(contact)
                .channelType(channelType)
                .externalUserId(externalUserId)
                .externalChatId(externalChatId)
                .username(username)
                .displayName(displayName)
                .build();

        return linkRepository.save(link);
    }

    public ChannelUserLink findByChannelAndExternalId(ChannelType channelType, String externalUserId) {
        if (externalUserId == null || externalUserId.isBlank()) {
            throw new IllegalArgumentException("externalUserId cannot be empty");
        }

        return linkRepository.findByChannelTypeAndExternalUserId(channelType, externalUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ChannelUserLink not found for " + channelType + " and externalUserId=" + externalUserId));
    }
}
