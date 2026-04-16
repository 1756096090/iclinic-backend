package com.iclinic.iclinicbackend.config;

import com.iclinic.iclinicbackend.modules.crm.channel.adapter.telegram.TelegramClient;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.security.SecretEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookAutoRegister implements ApplicationRunner {

    private final ChannelConnectionRepository channelConnectionRepository;
    private final TelegramClient telegramClient;
    private final SecretEncryptionService secretEncryptionService;
    private final TelegramWebhookBaseUrlResolver webhookBaseUrlResolver;

    @Value("${telegram.webhook.auto-register-enabled:false}")
    private boolean autoRegisterEnabled;

    @Value("${telegram.webhook.auto-register.max-attempts:6}")
    private int maxAttempts;

    @Value("${telegram.webhook.auto-register.retry-delay-seconds:5}")
    private long retryDelaySeconds;

    @Value("${telegram.webhook.auto-register.initial-delay-seconds:8}")
    private long initialDelaySeconds;

    private static final Set<ChannelConnectionStatus> OPERATIVE =
            Set.of(ChannelConnectionStatus.PENDING, ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED);

    @Override
    public void run(ApplicationArguments args) {
        if (!autoRegisterEnabled) {
            log.info("telegram.webhook.auto-register-enabled=false — omitiendo auto-registro de webhook");
            return;
        }

        String webhookBaseUrl = webhookBaseUrlResolver.resolveBaseUrl();
        if (webhookBaseUrl == null || webhookBaseUrl.isBlank()) {
            log.info("No hay URL publica de webhook disponible — omitiendo auto-registro de webhook");
            return;
        }

        List<ChannelConnection> telegramChannels = channelConnectionRepository
                .findByChannelTypeAndStatusInWithCompany(ChannelType.TELEGRAM, OPERATIVE);

        if (telegramChannels.isEmpty()) {
            log.info("No se encontraron canales Telegram activos para registrar webhook");
            return;
        }

        sleep(initialDelaySeconds);

        for (ChannelConnection channel : telegramChannels) {
            try {
                String token = secretEncryptionService.decrypt(channel.getAccessTokenEncrypted());
                String webhookUrl = buildUrl(webhookBaseUrl, channel.getCompany().getId());

                // Generamos un secret determinístico: SHA-256(botToken) → hex[:32]
                // Telegram requiere que el secret sea [A-Za-z0-9_-], 1-256 chars.
                String secretToken = computeWebhookSecret(token);

                registerWebhookWithRetry(token, webhookUrl, secretToken, channel.getId());

                // Persistimos solo despues de confirmar que Telegram acepto el webhook.
                channel.setWebhookVerifyToken(secretToken);
                channel.setWebhookRegisteredUrl(webhookUrl);
                channel.setStatus(ChannelConnectionStatus.ACTIVE);
                channelConnectionRepository.save(channel);

                log.info("✅ Telegram webhook registrado: channelId={} companyId={} url={}",
                        channel.getId(), channel.getCompany().getId(), webhookUrl);
            } catch (Exception e) {
                log.warn("⚠️  No se pudo registrar webhook para channelId={}: {}",
                        channel.getId(), e.getMessage());
            }
        }
    }

    private String buildUrl(String base, Long companyId) {
        String clean = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return clean + "/api/v1/crm/webhooks/telegram/" + companyId;
    }

    private void registerWebhookWithRetry(String token, String webhookUrl, String secretToken, Long channelId) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            try {
                telegramClient.setWebhook(token, webhookUrl, secretToken);
                if (attempt > 1) {
                    log.info("Webhook Telegram registrado exitosamente en intento {} para channelId={}", attempt, channelId);
                }
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                boolean isDnsError = ex.getMessage() != null &&
                        (ex.getMessage().contains("resolve host") || ex.getMessage().contains("Name or service not known"));
                long delay = isDnsError ? retryDelaySeconds * 2 : retryDelaySeconds;

                if (attempt < maxAttempts) {
                    log.warn("Intento {}/{} fallo para setWebhook channelId={}. Reintentando en {}s. Motivo: {}",
                            attempt, maxAttempts, channelId, delay, ex.getMessage());
                    sleep(delay);
                }
            }
        }

        throw lastError != null ? lastError : new RuntimeException("No se pudo registrar webhook");
    }

    private void sleep(long seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inicializacion de webhook interrumpida", e);
        }
    }

    /**
     * Deriva un token de 64 chars en hex a partir del bot token usando SHA-256.
     * Determinístico: mismo botToken → mismo secret (útil para reiniciar sin romper el webhook).
     */
    private String computeWebhookSecret(String botToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // 64 chars hex, dentro del límite de 256
        } catch (Exception e) {
            throw new RuntimeException("Error computando webhook secret", e);
        }
    }
}
