package com.iclinic.iclinicbackend.modules.crm.channel.adapter.telegram;

import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramApiResponseDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramGetWebhookInfoResponseDto;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram.TelegramSetWebhookResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramClient {

    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;

    public String sendMessage(String botToken, String chatId, String text) {
        String url = TELEGRAM_API_BASE_URL + botToken + "/sendMessage";

        try {
            TelegramSendMessageRequest request = new TelegramSendMessageRequest(chatId, text);
            TelegramApiResponseDto response = restTemplate.postForObject(url, request, TelegramApiResponseDto.class);

            if (response == null || !Boolean.TRUE.equals(response.getOk())) {
                throw new RuntimeException("Failed to send Telegram message");
            }

            Long messageId = response.getResult().getMessageId();
            log.info("Message sent successfully, messageId={}", messageId);
            return messageId.toString();
        } catch (Exception e) {
            log.error("Error calling Telegram API", e);
            throw new RuntimeException("Error communicating with Telegram API", e);
        }
    }

    /**
     * Valida el token llamando a getMe. Retorna el username del bot (@username).
     * Lanza RuntimeException si el token es inválido.
     */
    public TelegramGetWebhookInfoResponseDto getWebhookInfo(String botToken) {
        String url = TELEGRAM_API_BASE_URL + botToken + "/getWebhookInfo";
        try {
            TelegramGetWebhookInfoResponseDto response = restTemplate.getForObject(url, TelegramGetWebhookInfoResponseDto.class);
            if (response == null || !Boolean.TRUE.equals(response.getOk())) {
                throw new RuntimeException("Telegram getWebhookInfo returned not-ok response");
            }
            log.info("Webhook info retrieved for token ending in ...{}", botToken.length() > 6 ? botToken.substring(botToken.length() - 6) : "***");
            return response;
        } catch (Exception e) {
            log.error("Error calling Telegram getWebhookInfo API", e);
            throw new RuntimeException("Error retrieving Telegram webhook info", e);
        }
    }

    public String validateToken(String botToken) {
        String url = TELEGRAM_API_BASE_URL + botToken + "/getMe";
        try {
            TelegramApiResponseDto response = restTemplate.getForObject(url, TelegramApiResponseDto.class);
            if (response == null || !Boolean.TRUE.equals(response.getOk()) || response.getResult() == null) {
                throw new RuntimeException("Invalid Telegram bot token");
            }
            String username = response.getResult().getUsername();
            log.info("Telegram token valid, bot username=@{}", username);
            return "@" + username;
        } catch (Exception e) {
            log.error("Error validating Telegram token via getMe", e);
            throw new RuntimeException("Invalid or unreachable Telegram bot token", e);
        }
    }

    public void setWebhook(String botToken, String webhookUrl, String secretToken) {
        StringBuilder urlBuilder = new StringBuilder(TELEGRAM_API_BASE_URL)
                .append(botToken)
                .append("/setWebhook?url=")
                .append(webhookUrl);

        if (secretToken != null && !secretToken.isBlank()) {
            urlBuilder.append("&secret_token=").append(secretToken);
        }

        try {
            TelegramSetWebhookResponseDto response = restTemplate.postForObject(
                    urlBuilder.toString(), null, TelegramSetWebhookResponseDto.class);
            if (response == null || !Boolean.TRUE.equals(response.getOk())) {
                String desc = response != null ? response.getDescription() : "null response";
                throw new RuntimeException("Telegram setWebhook returned not-ok response: " + desc);
            }
            log.info("Webhook registered successfully: url={}", webhookUrl);
        } catch (Exception e) {
            log.error("Error calling Telegram setWebhook API", e);
            throw new RuntimeException("Error registering Telegram webhook", e);
        }
    }

    static class TelegramSendMessageRequest {
        public String chat_id;
        public String text;

        TelegramSendMessageRequest(String chatId, String text) {
            this.chat_id = chatId;
            this.text = text;
        }
    }
}
