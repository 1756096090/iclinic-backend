package com.iclinic.iclinicbackend.modules.crm.message.service;

import com.iclinic.iclinicbackend.modules.crm.channel.adapter.MessagingChannelAdapter;
import com.iclinic.iclinicbackend.modules.crm.channel.adapter.MessagingChannelAdapterRegistry;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelUserLinkRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.service.CrmContactService;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.conversation.service.ConversationService;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.modules.crm.message.dto.SendMessageRequestDto;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.modules.crm.message.repository.CrmMessageRepository;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.InboundWebhookMessageDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.MessageDirection;
import com.iclinic.iclinicbackend.shared.enums.MessageStatus;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private static final Set<ChannelConnectionStatus> OPERATIVE_STATUSES =
            Set.of(ChannelConnectionStatus.PENDING, ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED);

    private final CrmMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ChannelUserLinkRepository channelUserLinkRepository;
    private final UserRepository userRepository;
    private final ChannelConnectionRepository channelConnectionRepository;
    private final CrmContactService contactService;
    private final ConversationService conversationService;
    private final MessagingChannelAdapterRegistry adapterRegistry;

    @Override
    public CrmMessage sendText(SendMessageRequestDto dto) {
        log.info("Sending outbound message for conversation={}", dto.getConversationId());
        
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.MessageContentInvalidException(
                    "El contenido del mensaje no puede estar vacío");
        }

        Conversation conversation = conversationRepository.findById(dto.getConversationId())
                .orElseThrow(() -> new com.iclinic.iclinicbackend.modules.crm.exception.ConversationNotFoundException(
                        dto.getConversationId()));

        User user = userRepository.findById(dto.getSentByUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + dto.getSentByUserId()));

        ChannelConnection connection = conversation.getChannelConnection();
        CrmContact contact = conversation.getContact();

        ChannelUserLink channelUserLink = resolveChannelUserLink(conversation, contact, connection);

        MessagingChannelAdapter adapter = adapterRegistry.get(connection.getChannelType());
        String externalMessageId;
        
        try {
            externalMessageId = adapter.sendText(connection, channelUserLink.getExternalChatId(), dto.getContent());
        } catch (Exception e) {
            log.error("Failed to send message through adapter", e);
            throw new RuntimeException("Error sending message through channel", e);
        }

        CrmMessage message = CrmMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.OUTBOUND)
                .channelType(connection.getChannelType())
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .externalMessageId(externalMessageId)
                .content(dto.getContent())
                .sentByUser(user)
                .build();

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);
        
        CrmMessage savedMessage = messageRepository.save(message);
        log.info("Outbound message sent and saved, messageId={}", savedMessage.getId());
        
        return savedMessage;
    }

        private ChannelUserLink resolveChannelUserLink(
                        Conversation conversation,
                        CrmContact contact,
                        ChannelConnection connection) {

                return channelUserLinkRepository
                                .findByContactAndChannelType(contact, connection.getChannelType())
                                .orElseGet(() -> createLinkWhenPossible(conversation, contact, connection));
        }

        private ChannelUserLink createLinkWhenPossible(
                        Conversation conversation,
                        CrmContact contact,
                        ChannelConnection connection) {

                if (ChannelType.WHATSAPP.equals(connection.getChannelType())) {
                        String phone = normalizePhoneForExternalId(contact.getPhone());
                        if (phone == null || phone.isBlank()) {
                                throw new InvalidChannelConfigurationException(
                                                "La conversacion " + conversation.getId() + " no tiene ChannelUserLink y el contacto "
                                                                + contact.getId() + " no tiene telefono valido para autovincular WhatsApp");
                        }

                        ChannelUserLink link = ChannelUserLink.builder()
                                        .contact(contact)
                                        .channelType(connection.getChannelType())
                                        .externalUserId(phone)
                                        .externalChatId(phone)
                                        .displayName(contact.getFullName())
                                        .build();

                        ChannelUserLink saved = channelUserLinkRepository.save(link);
                        log.info("ChannelUserLink autogenerado para WhatsApp: conversationId={} contactId={} phone={}",
                                        conversation.getId(), contact.getId(), phone);
                        return saved;
                }

                throw new InvalidChannelConfigurationException(
                                "La conversacion " + conversation.getId() + " no tiene ChannelUserLink para "
                                                + connection.getChannelType()
                                                + ". Para Telegram primero debe llegar un mensaje entrante real del contacto para registrar chat_id/user_id.");
        }

        private String normalizePhoneForExternalId(String raw) {
                if (raw == null) {
                        return null;
                }
                String digits = raw.replaceAll("[^0-9]", "");
                return digits.isBlank() ? null : digits;
        }

    @Override
    public void processInboundMessage(InboundWebhookMessageDto dto) {
        log.info("Processing inbound message from phone={} channel={}", dto.getContactPhone(), dto.getChannelType());
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.MessageContentInvalidException(
                    "El mensaje entrante no tiene contenido");
        }

        ChannelConnection connection = channelConnectionRepository
                .findActiveByCompanyAndChannel(
                        dto.getCompanyId(),
                        dto.getChannelType(),
                        OPERATIVE_STATUSES)
                .orElseThrow(() -> new com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException(
                        "Canal operativo no encontrado para companyId=" + dto.getCompanyId()));

        IncomingChannelMessage incomingMsg = IncomingChannelMessage.builder()
                .companyId(dto.getCompanyId())
                .channelType(dto.getChannelType())
                .externalUserId(dto.getContactPhone())
                .externalChatId(dto.getContactPhone())
                .externalMessageId(dto.getExternalMessageId())
                .displayName(dto.getContactName() != null ? dto.getContactName() : "Desconocido")
                .phone(dto.getContactPhone())
                .build();
        CrmContact contact = contactService.resolveContact(connection, incomingMsg);

        Conversation conversation = conversationService.findOrCreateOpenConversation(contact, connection);

        CrmMessage message = CrmMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .channelType(dto.getChannelType())
                .messageType(MessageType.TEXT)
                .status(MessageStatus.RECEIVED)
                .externalMessageId(dto.getExternalMessageId())
                .content(dto.getContent())
                .build();

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);
        messageRepository.save(message);
    }

    @Override
    public List<CrmMessage> findByConversation(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public List<CrmMessage> findAll() {
        return messageRepository.findAll();
    }
}
