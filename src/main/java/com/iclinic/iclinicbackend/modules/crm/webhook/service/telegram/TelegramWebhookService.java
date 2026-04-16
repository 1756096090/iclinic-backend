package com.iclinic.iclinicbackend.modules.crm.webhook.service.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.crm.exception.DuplicateMessageException;
import com.iclinic.iclinicbackend.modules.crm.exception.TelegramWebhookException;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramUpdateDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.service.ChannelInboundMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookService {

    private final ChannelInboundMessageProcessor messageProcessor;
    private final TelegramUpdateParser updateParser;
    private final ObjectMapper objectMapper;

    public void processIncomingUpdate(String payload, Long companyId) {
        log.info("Telegram webhook recibido: companyId={}", companyId);

        TelegramUpdateDto update = deserialize(payload);
        log.debug("Update deserializado: updateId={}", update.getUpdateId());

        Optional<IncomingChannelMessage> maybeMessage = updateParser.parse(update, companyId);

        if (maybeMessage.isEmpty()) {
            log.debug("Update {} ignorado por el parser", update.getUpdateId());
            return;
        }

        try {
            messageProcessor.processInboundMessage(maybeMessage.get());
            log.info("Telegram update {} procesado correctamente para companyId={}",
                    update.getUpdateId(), companyId);
        } catch (DuplicateMessageException e) {
            log.info("Update {} ya procesado anteriormente (idempotente): {}",
                    update.getUpdateId(), e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Update {} inválido: {}", update.getUpdateId(), e.getMessage());
        }
    }

    private TelegramUpdateDto deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TelegramUpdateDto.class);
        } catch (Exception e) {
            log.error("Error deserializando payload de Telegram: {}", e.getMessage());
            throw new TelegramWebhookException("Payload de Telegram inválido o malformado", e);
        }
    }
}
