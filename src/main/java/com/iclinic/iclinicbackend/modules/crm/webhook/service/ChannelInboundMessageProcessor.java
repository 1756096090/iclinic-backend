package com.iclinic.iclinicbackend.modules.crm.webhook.service;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.service.CrmContactService;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.conversation.service.ConversationService;
import com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionActiveNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.DuplicateMessageException;
import com.iclinic.iclinicbackend.modules.crm.message.dto.MessageNotificationDto;
import com.iclinic.iclinicbackend.modules.crm.message.dto.NewMessageEvent;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.modules.crm.message.repository.CrmMessageRepository;
import com.iclinic.iclinicbackend.modules.crm.message.service.MessageEventPublisher;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.MessageDirection;
import com.iclinic.iclinicbackend.shared.enums.MessageStatus;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChannelInboundMessageProcessor {

    private static final Set<ChannelConnectionStatus> OPERATIVE_STATUSES =
            Set.of(ChannelConnectionStatus.VERIFIED, ChannelConnectionStatus.ACTIVE);

    private final CrmMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ChannelConnectionRepository connectionRepository;
    private final CrmContactService contactService;
    private final ConversationService conversationService;
    private final MessageEventPublisher eventPublisher;
    private final SimpMessagingTemplate wsTemplate;

    public CrmMessage processInboundMessage(IncomingChannelMessage msg) {
        log.info("Procesando mensaje entrante: channel={} externalUserId={} externalMessageId={}",
                msg.getChannelType(), msg.getExternalUserId(), msg.getExternalMessageId());

        msg.validate();
        checkDuplicate(msg);

        ChannelConnection connection = findOperativeConnection(msg);
        CrmContact contact = contactService.resolveContact(connection, msg);

        boolean isNewConversation = !conversationService.hasOpenConversation(contact, connection);
        Conversation conversation = conversationService.findOrCreateOpenConversation(contact, connection);

        CrmMessage saved = saveMessage(msg, conversation);

        Instant messageTimestamp = msg.getTimestamp() != null ? msg.getTimestamp() : Instant.now();
        conversation.setLastMessageAt(messageTimestamp);
        conversationRepository.save(conversation);

        log.info("Mensaje persistido: messageId={} contactId={} conversationId={}",
                saved.getId(), contact.getId(), conversation.getId());

        pushWebSocket(saved, contact, msg, isNewConversation);
        pushSse(saved, contact.getId(), msg);

        return saved;
    }

    private void pushWebSocket(CrmMessage saved, CrmContact contact,
                               IncomingChannelMessage msg, boolean isNewConversation) {
        try {
            MessageNotificationDto notification = MessageNotificationDto.builder()
                    .messageId(saved.getId())
                    .conversationId(saved.getConversation().getId())
                    .contactId(contact.getId())
                    .contactName(contact.getFullName())
                    .channelType(msg.getChannelType())
                    .preview(buildPreview(msg))
                    .receivedAt(saved.getCreatedAt())
                    .newConversation(isNewConversation)
                    .build();

            String destination = "/topic/notifications/" + msg.getCompanyId();
            wsTemplate.convertAndSend(destination, notification);
            log.info("WebSocket notification sent → {} (messageId={})", destination, saved.getId());
        } catch (Exception e) {
            log.warn("WebSocket push failed (non-critical): {}", e.getMessage());
        }
    }

    private void pushSse(CrmMessage saved, Long contactId, IncomingChannelMessage msg) {
        try {
            NewMessageEvent event = NewMessageEvent.builder()
                    .messageId(saved.getId())
                    .conversationId(saved.getConversation().getId())
                    .contactId(contactId)
                    .companyId(msg.getCompanyId())
                    .channelType(msg.getChannelType())
                    .direction(MessageDirection.INBOUND)
                    .messageType(msg.getMessageType())
                    .content(msg.getText())
                    .createdAt(saved.getCreatedAt())
                    .build();
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("SSE publish failed (non-critical): {}", e.getMessage());
        }
    }

    private String buildPreview(IncomingChannelMessage msg) {
        if (msg.getMessageType() == MessageType.IMAGE)    return "[Imagen]";
        if (msg.getMessageType() == MessageType.LOCATION) return "[Ubicación]";
        if (msg.getMessageType() == MessageType.STICKER)  return "[Sticker]";
        if (msg.getMessageType() == MessageType.CONTACT)  return "[Contacto]";
        if (msg.getText() == null) return "[Mensaje]";
        String text = msg.getText().trim();
        return text.length() > 80 ? text.substring(0, 77) + "..." : text;
    }

    private void checkDuplicate(IncomingChannelMessage msg) {
        messageRepository.findByChannelTypeAndExternalMessageId(
                        msg.getChannelType(), msg.getExternalMessageId())
                .ifPresent(existing -> {
                    log.warn("Mensaje duplicado: externalMessageId={}", msg.getExternalMessageId());
                    throw new DuplicateMessageException(msg.getExternalMessageId());
                });
    }

    private ChannelConnection findOperativeConnection(IncomingChannelMessage msg) {
        return connectionRepository
                .findActiveByCompanyAndChannel(msg.getCompanyId(), msg.getChannelType(), OPERATIVE_STATUSES)
                .orElseThrow(() -> new ChannelConnectionActiveNotFoundException(
                        msg.getCompanyId(), msg.getChannelType()));
    }

    private CrmMessage saveMessage(IncomingChannelMessage msg, Conversation conversation) {
        CrmMessage message = CrmMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .channelType(msg.getChannelType())
                .messageType(msg.getMessageType())
                .status(MessageStatus.RECEIVED)
                .externalMessageId(msg.getExternalMessageId())
                .content(msg.getText())
                .build();
        return messageRepository.save(message);
    }
}
