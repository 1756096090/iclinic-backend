package com.iclinic.iclinicbackend.modules.crm.webhook.controller;

import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.modules.crm.webhook.service.telegram.TelegramWebhookService;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/crm/webhooks/telegram")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM Webhooks", description = "Endpoints de webhook para Telegram")
public class TelegramWebhookController {

    private final TelegramWebhookService telegramWebhookService;
    private final ChannelConnectionRepository channelConnectionRepository;

    private static final Set<ChannelConnectionStatus> OPERATIVE =
            Set.of(ChannelConnectionStatus.PENDING, ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED);

    @PostMapping("/{companyId}")
    @Operation(summary = "Recibir actualizaciones de Telegram",
            description = "Telegram envía mensajes entrantes aquí")
    public ResponseEntity<Void> receive(
            @PathVariable Long companyId,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretHeader,
            @RequestBody String payload) {

        validateTelegramSecret(companyId, secretHeader);

        log.info("Received Telegram webhook for companyId={}", companyId);
        telegramWebhookService.processIncomingUpdate(payload, companyId);
        return ResponseEntity.ok().build();
    }

    private void validateTelegramSecret(Long companyId, String providedSecret) {
        ChannelConnection connection = channelConnectionRepository
                .findActiveByCompanyAndChannel(companyId, ChannelType.TELEGRAM, OPERATIVE)
                .orElseThrow(() -> new InvalidChannelConfigurationException(
                        "No existe conexión Telegram operativa para companyId=" + companyId));

        String expectedSecret = connection.getWebhookVerifyToken();

        if (expectedSecret == null || expectedSecret.isBlank()) {
            throw new InvalidChannelConfigurationException(
                    "Conexión Telegram sin secret token configurado para companyId=" + companyId);
        }

        if (providedSecret == null || !expectedSecret.equals(providedSecret)) {
            throw new InvalidChannelConfigurationException("Secret token inválido para webhook Telegram");
        }
    }
}
