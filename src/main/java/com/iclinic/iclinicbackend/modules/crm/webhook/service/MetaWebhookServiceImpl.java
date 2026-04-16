package com.iclinic.iclinicbackend.modules.crm.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.message.service.MessageService;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.InboundWebhookMessageDto;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaWebhookServiceImpl implements MetaWebhookService {

    private static final Set<ChannelConnectionStatus> OPERATIVE =
            Set.of(ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED);

    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final ChannelConnectionRepository channelConnectionRepository;

    @Override
    public String verifyWebhook(String mode, String token, String challenge) {
        if (!"subscribe".equals(mode)) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException("Modo inválido: " + mode);
        }
        channelConnectionRepository.findByWebhookVerifyToken(token)
                .orElseThrow(() -> new com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException("Token de verificación inválido"));

        log.info("Webhook verificado con token={}", token);
        return challenge;
    }

    @Override
    public void processIncomingPayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            String from = root.at("/entry/0/changes/0/value/messages/0/from").asText();
            String text = root.at("/entry/0/changes/0/value/messages/0/text/body").asText();
            String name = root.at("/entry/0/changes/0/value/contacts/0/profile/name").asText();
            String externalMessageId = root.at("/entry/0/changes/0/value/messages/0/id").asText();
            String businessAccountId = root.at("/entry/0/changes/0/value/metadata/phone_number_id").asText();

            if (businessAccountId.isBlank()) {
                throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException(
                        "phone_number_id no presente en el payload de Meta");
            }

            // Resolvemos el companyId desde la conexión de canal, nunca del payload
            ChannelConnection connection = channelConnectionRepository
                    .findByExternalPhoneNumberIdAndChannelTypeAndStatusInWithCompany(
                            businessAccountId,
                            ChannelType.WHATSAPP,
                            OPERATIVE)
                    .orElseThrow(() -> new com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException(
                            "No se encontró conexión WhatsApp operativa para phone_number_id=" + businessAccountId));
            long companyId = connection.getCompany().getId();

            log.info("Inbound WhatsApp message from={} externalId={} businessAccountId={} companyId={}",
                    from, externalMessageId, businessAccountId, companyId);

            InboundWebhookMessageDto dto = InboundWebhookMessageDto.builder()
                    .channelType(ChannelType.WHATSAPP)
                    .companyId(companyId)
                    .contactName(name)
                    .contactPhone(from)
                    .externalMessageId(externalMessageId)
                    .content(text)
                    .build();

            messageService.processInboundMessage(dto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException("No se pudo procesar el webhook: " + e.getMessage());
        }
    }
}
