package com.iclinic.iclinicbackend.modules.crm.channel.service;

import com.iclinic.iclinicbackend.config.TelegramWebhookBaseUrlResolver;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.adapter.telegram.TelegramClient;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.ChannelConnectionResponseDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.CreateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.UpdateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.WebhookInfoDto;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramGetWebhookInfoResponseDto;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChannelConnectionServiceImpl implements ChannelConnectionService {

    private final ChannelConnectionRepository channelConnectionRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final com.iclinic.iclinicbackend.shared.security.SecretEncryptionService secretEncryptionService;
    private final TelegramClient telegramClient;
    private final TelegramWebhookBaseUrlResolver webhookBaseUrlResolver;

    @Override
    public List<ChannelConnectionResponseDto> getAll() {
        return channelConnectionRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null)
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public ChannelConnectionResponseDto create(CreateChannelConnectionRequestDto dto) {
        log.info("Creating channel connection type={} for company={}", dto.getChannelType(), dto.getCompanyId());

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException(dto.getCompanyId()));

        Branch branch = null;
        if (dto.getBranchId() != null) {
            branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException(dto.getBranchId()));
            if (!branch.getCompany().getId().equals(company.getId())) {
                throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException("La sucursal no pertenece a la empresa");
            }
        }

        ChannelConnectionStatus status = ChannelConnectionStatus.PENDING;
        String externalAccountId = dto.getExternalAccountId();
        String webhookUrl = null;
        String webhookVerifyToken = dto.getWebhookVerifyToken();

        if (ChannelType.TELEGRAM.equals(dto.getChannelType())) {
            String botUsername = telegramClient.validateToken(dto.getAccessToken());
            externalAccountId = botUsername; // usar username del bot como identificador externo

            String baseUrl = dto.getWebhookBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = webhookBaseUrlResolver.resolveBaseUrl();
            }

            if (baseUrl != null && !baseUrl.isBlank()) {
                webhookUrl = buildWebhookUrl(baseUrl, company.getId());
                webhookVerifyToken = resolveTelegramWebhookSecret(dto.getWebhookVerifyToken(), dto.getAccessToken());
                telegramClient.setWebhook(dto.getAccessToken(), webhookUrl, webhookVerifyToken);
                status = ChannelConnectionStatus.ACTIVE;
                log.info("Telegram webhook configured: {}", webhookUrl);
            }
        }

        ChannelConnection connection = ChannelConnection.builder()
                .company(company)
                .branch(branch)
                .channelType(dto.getChannelType())
                .provider(dto.getProvider())
                .externalAccountId(externalAccountId)
                .externalPhoneNumberId(dto.getExternalPhoneNumberId())
                .accessTokenEncrypted(secretEncryptionService.encrypt(dto.getAccessToken()))
                .webhookVerifyToken(webhookVerifyToken)
                .webhookRegisteredUrl(webhookUrl)
                .status(status)
                .build();

        return toResponseDto(channelConnectionRepository.save(connection));
    }

    private String buildWebhookUrl(String baseUrl, Long companyId) {
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBase + "/api/v1/crm/webhooks/telegram/" + companyId;
    }

    @Override
    public ChannelConnectionResponseDto update(Long id, UpdateChannelConnectionRequestDto dto) {
        ChannelConnection connection = load(id);

        if (dto.getAccessToken() != null && !dto.getAccessToken().isBlank()) {
            if (ChannelType.TELEGRAM.equals(connection.getChannelType())) {
                String botUsername = telegramClient.validateToken(dto.getAccessToken());
                connection.setExternalAccountId(botUsername);
                log.info("Telegram token actualizado para channelId={}, botUsername={}", id, botUsername);
            }
            connection.setAccessTokenEncrypted(secretEncryptionService.encrypt(dto.getAccessToken()));
        }

        if (dto.getWebhookVerifyToken() != null && !dto.getWebhookVerifyToken().isBlank()) {
            connection.setWebhookVerifyToken(dto.getWebhookVerifyToken());
        }

        if (dto.getWebhookBaseUrl() != null && !dto.getWebhookBaseUrl().isBlank()) {
            String token = secretEncryptionService.decrypt(connection.getAccessTokenEncrypted());
            String registeredUrl = buildWebhookUrl(dto.getWebhookBaseUrl(), connection.getCompany().getId());
            String secret = resolveTelegramWebhookSecret(connection.getWebhookVerifyToken(), token);

            telegramClient.setWebhook(token, registeredUrl, secret);
            connection.setWebhookVerifyToken(secret);
            connection.setWebhookRegisteredUrl(registeredUrl);
            connection.setStatus(ChannelConnectionStatus.ACTIVE);
            log.info("Telegram webhook re-registrado: channelId={} url={}", id, registeredUrl);
        }

        return toResponseDto(channelConnectionRepository.save(connection));
    }

    @Override
    public ChannelConnectionResponseDto activate(Long id) {
        ChannelConnection connection = load(id);
        connection.setStatus(ChannelConnectionStatus.ACTIVE);
        return toResponseDto(channelConnectionRepository.save(connection));
    }

    @Override
    public ChannelConnectionResponseDto deactivate(Long id) {
        ChannelConnection connection = load(id);
        connection.setStatus(ChannelConnectionStatus.INACTIVE);
        return toResponseDto(channelConnectionRepository.save(connection));
    }

    @Override
    public List<ChannelConnectionResponseDto> findByCompany(Long companyId) {
        return channelConnectionRepository.findByCompanyId(companyId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        ChannelConnection connection = load(id);
        connection.setDeletedAt(LocalDateTime.now());
        channelConnectionRepository.save(connection);
        log.info("Canal eliminado lógicamente: channelId={}", id);
    }

    @Override
    public ChannelConnectionResponseDto setWebhook(Long id, String webhookBaseUrl) {
        ChannelConnection connection = load(id);

        if (!ChannelType.TELEGRAM.equals(connection.getChannelType())) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException(
                    "setWebhook solo está disponible para canales de tipo TELEGRAM");
        }

        String plainToken = secretEncryptionService.decrypt(connection.getAccessTokenEncrypted());
        String webhookUrl = buildWebhookUrl(webhookBaseUrl, connection.getCompany().getId());
        String secret = resolveTelegramWebhookSecret(connection.getWebhookVerifyToken(), plainToken);

        telegramClient.setWebhook(plainToken, webhookUrl, secret);
        connection.setWebhookVerifyToken(secret);
        connection.setWebhookRegisteredUrl(webhookUrl);
        log.info("Webhook de Telegram registrado: channelId={} url={}", id, webhookUrl);

        connection.setStatus(ChannelConnectionStatus.ACTIVE);
        return toResponseDto(channelConnectionRepository.save(connection));
    }

    @Override
    public WebhookInfoDto getWebhookInfo(Long id) {
        ChannelConnection connection = load(id);

        if (!ChannelType.TELEGRAM.equals(connection.getChannelType())) {
            throw new com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException(
                    "getWebhookInfo solo está disponible para canales de tipo TELEGRAM");
        }

        String plainToken = secretEncryptionService.decrypt(connection.getAccessTokenEncrypted());
        TelegramGetWebhookInfoResponseDto response = telegramClient.getWebhookInfo(plainToken);
        TelegramGetWebhookInfoResponseDto.WebhookInfoResult result = response.getResult();

        // Use the URL that was actually registered for this connection (falls back to app property).
        String expectedUrl = connection.getWebhookRegisteredUrl() != null
                ? connection.getWebhookRegisteredUrl()
            : resolveExpectedUrl(connection.getCompany().getId());

        return WebhookInfoDto.builder()
                .url(result.getUrl())
                .hasCustomCertificate(result.isHasCustomCertificate())
                .pendingUpdateCount(result.getPendingUpdateCount())
                .lastErrorDate(result.getLastErrorDate())
                .lastErrorMessage(result.getLastErrorMessage())
                .maxConnections(result.getMaxConnections())
                .urlMatches(expectedUrl.equals(result.getUrl()))
                .expectedUrl(expectedUrl)
                .build();
    }

    private String resolveExpectedUrl(Long companyId) {
        String baseUrl = webhookBaseUrlResolver.resolveBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return buildWebhookUrl(baseUrl, companyId);
    }

    private ChannelConnection load(Long id) {
        return channelConnectionRepository.findById(id)
                .orElseThrow(() -> new com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException(id));
    }

    private ChannelConnectionResponseDto toResponseDto(ChannelConnection c) {
        return ChannelConnectionResponseDto.builder()
                .id(c.getId())
                .branchId(c.getBranch() != null ? c.getBranch().getId() : null)
                .channelType(c.getChannelType())
                .provider(c.getProvider())
                .externalAccountId(c.getExternalAccountId())
                .externalPhoneNumberId(c.getExternalPhoneNumberId())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private String resolveTelegramWebhookSecret(String currentSecret, String botToken) {
        if (currentSecret != null && !currentSecret.isBlank()) {
            return currentSecret;
        }
        return computeWebhookSecret(botToken);
    }

    private String computeWebhookSecret(String botToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar secret token de Telegram", e);
        }
    }
}
