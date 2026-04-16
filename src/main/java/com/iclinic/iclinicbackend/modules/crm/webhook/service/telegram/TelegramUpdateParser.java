package com.iclinic.iclinicbackend.modules.crm.webhook.service.telegram;

import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramContactDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramMessageDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramUpdateDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramUserDto;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class TelegramUpdateParser {

    public Optional<IncomingChannelMessage> parse(TelegramUpdateDto update, Long companyId) {
        TelegramMessageDto message = update.resolveMessage();

        if (message == null) {
            log.debug("Update {} no contiene mensaje procesable, ignorando", update.getUpdateId());
            return Optional.empty();
        }
        if (message.getFrom() == null) {
            log.debug("Update {} sin remitente, ignorando", update.getUpdateId());
            return Optional.empty();
        }
        if (message.getChat() == null) {
            log.warn("Update {} sin información de chat, ignorando", update.getUpdateId());
            return Optional.empty();
        }
        if (!message.hasContent()) {
            log.debug("Update {} sin contenido procesable, ignorando", update.getUpdateId());
            return Optional.empty();
        }

        TelegramUserDto sender = message.getFrom();
        MessageType messageType = resolveMessageType(message);

        IncomingChannelMessage incoming = IncomingChannelMessage.builder()
                .companyId(companyId)
                .channelType(ChannelType.TELEGRAM)
                .externalUserId(String.valueOf(sender.getId()))
                .externalChatId(String.valueOf(message.getChat().getId()))
                .externalMessageId(String.valueOf(message.getMessageId()))
                .displayName(buildDisplayName(sender))
                .username(sender.getUsername())
                .text(message.resolveText())
                .messageType(messageType)
                .phone(extractSharedPhone(message))
                .timestamp(Instant.ofEpochSecond(message.getDate()))
                .build();

        log.debug("Update {} mapeado: channel=TELEGRAM externalUserId={} type={} edited={}",
                update.getUpdateId(), sender.getId(), messageType, update.isEdited());

        return Optional.of(incoming);
    }

    private String buildDisplayName(TelegramUserDto user) {
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null) name.append(user.getFirstName().trim());
        if (user.getLastName() != null) {
            if (!name.isEmpty()) name.append(" ");
            name.append(user.getLastName().trim());
        }
        if (name.isEmpty() && user.getUsername() != null) name.append(user.getUsername());
        return name.toString().isBlank() ? "Telegram User" : name.toString();
    }

    private MessageType resolveMessageType(TelegramMessageDto msg) {
        if (msg.resolveText() != null) return MessageType.TEXT;
        if (msg.getPhoto() != null)    return MessageType.IMAGE;
        if (msg.getSticker() != null)  return MessageType.STICKER;
        if (msg.getLocation() != null) return MessageType.LOCATION;
        if (msg.getContact() != null)  return MessageType.CONTACT;
        return MessageType.TEXT;
    }

    /** Solo extrae teléfono cuando el usuario comparte su propio contacto. */
    private String extractSharedPhone(TelegramMessageDto msg) {
        TelegramContactDto contact = msg.getContact();
        if (contact == null) return null;
        if (contact.getUserId() != null
                && msg.getFrom() != null
                && contact.getUserId().equals(msg.getFrom().getId())
                && contact.getPhoneNumber() != null) {
            return contact.getPhoneNumber();
        }
        return null;
    }
}
