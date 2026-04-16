package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta de Telegram para el endpoint {@code /getWebhookInfo}.
 *
 * <p>Ejemplo de respuesta:</p>
 * <pre>{@code
 * {
 *   "ok": true,
 *   "result": {
 *     "url": "https://example.com/api/v1/crm/webhooks/telegram/1",
 *     "has_custom_certificate": false,
 *     "pending_update_count": 0,
 *     "last_error_date": 1712000000,
 *     "last_error_message": "Connection refused",
 *     "max_connections": 40
 *   }
 * }
 * }</pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramGetWebhookInfoResponseDto {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("result")
    private WebhookInfoResult result;

    @JsonProperty("description")
    private String description;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookInfoResult {

        @JsonProperty("url")
        private String url;

        @JsonProperty("has_custom_certificate")
        private boolean hasCustomCertificate;

        @JsonProperty("pending_update_count")
        private int pendingUpdateCount;

        @JsonProperty("last_error_date")
        private Long lastErrorDate;

        @JsonProperty("last_error_message")
        private String lastErrorMessage;

        @JsonProperty("max_connections")
        private int maxConnections;
    }
}
