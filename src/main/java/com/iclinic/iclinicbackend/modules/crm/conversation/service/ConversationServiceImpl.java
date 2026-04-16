package com.iclinic.iclinicbackend.modules.crm.conversation.service;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.exception.ConversationNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    public Conversation findOrCreateOpenConversation(CrmContact contact, ChannelConnection channelConnection) {
        if (!contact.getCompany().getId().equals(channelConnection.getCompany().getId())) {
            throw new InvalidChannelConfigurationException("Contacto y canal pertenecen a empresas distintas");
        }
        if (contact.getBranch() != null && channelConnection.getBranch() != null
                && !contact.getBranch().getId().equals(channelConnection.getBranch().getId())) {
            throw new InvalidChannelConfigurationException("El contacto y el canal deben pertenecer a la misma sucursal");
        }

        return conversationRepository.findFirstByContactIdAndStatusOrderByCreatedAtDesc(
                        contact.getId(), ConversationStatus.OPEN)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .contact(contact)
                                .channelConnection(channelConnection)
                                .status(ConversationStatus.OPEN)
                                .lastMessageAt(Instant.now())
                                .build()
                ));
    }

    @Override
    public boolean hasOpenConversation(CrmContact contact, ChannelConnection channelConnection) {
        return conversationRepository.findFirstByContactIdAndStatusOrderByCreatedAtDesc(
                contact.getId(), ConversationStatus.OPEN).isPresent();
    }

    @Override
    public Conversation assign(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        conversation.setAssignedUser(user);
        return conversationRepository.save(conversation);
    }

    @Override
    public List<Conversation> findByBranch(Long branchId) {
        return conversationRepository.findByContactBranchIdOrderByLastMessageAtDesc(branchId);
    }

    @Override
    public List<Conversation> findAll() {
        return conversationRepository.findAll();
    }

    @Override
    public List<Conversation> findByCompany(Long companyId) {
        return conversationRepository.findByContactCompanyIdAndStatusOrderByLastMessageAtDesc(
                companyId, ConversationStatus.OPEN);
    }

    @Override
    public Conversation close(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.setStatus(ConversationStatus.CLOSED);
        return conversationRepository.save(conversation);
    }
}
