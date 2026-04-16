package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta de Telegram para el endpoint {@code /setWebhook}.
 *
 * <p>A diferencia de otros endpoints, Telegram devuelve {@code "result": true}
 * (un boolean primitivo), no un objeto JSON. Por eso este DTO modela
 * {@code result} como {@link Boolean} y no como un objeto anidado.</p>
 *
 * <p>Ejemplo de respuesta exitosa:</p>
 * <pre>{@code
 * {
 *   "ok": true,
 *   "result": true,
 *   "description": "Webhook was set"
 * }
 * }</pre>
 *
 * <p>Ejemplo de respuesta fallida:</p>
 * <pre>{@code
 * {
 *   "ok": false,
 *   "result": false,
 *   "description": "Bad webhook: HTTPS URL must have a host"
 * }
 * }</pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramSetWebhookResponseDto {

    /** {@code true} si la llamada fue exitosa según Telegram. */
    @JsonProperty("ok")
    private Boolean ok;

    /**
     * Resultado de la operación.
     * Para {@code setWebhook} Telegram siempre devuelve un boolean
     * (no un objeto), a diferencia de otros endpoints.
     */
    @JsonProperty("result")
    private Boolean result;

    /**
     * Mensaje descriptivo que Telegram incluye, especialmente útil
     * cuando {@code ok=false} para explicar el motivo del error.
     */
    @JsonProperty("description")
    private String description;
}

