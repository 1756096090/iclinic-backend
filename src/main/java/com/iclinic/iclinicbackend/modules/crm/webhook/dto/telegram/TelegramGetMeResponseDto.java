package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta de Telegram para el endpoint {@code /getMe}.
 *
 * <p>Se utiliza para validar el token de un bot. Telegram devuelve
 * {@code "result"} como un objeto {@link BotInfoDto} con los datos del bot.</p>
 *
 * <p>Ejemplo de respuesta:</p>
 * <pre>{@code
 * {
 *   "ok": true,
 *   "result": {
 *     "id": 123456789,
 *     "is_bot": true,
 *     "first_name": "MyBot",
 *     "username": "MyBot_bot",
 *     "can_join_groups": true,
 *     "can_read_all_group_messages": false,
 *     "supports_inline_queries": false
 *   }
 * }
 * }</pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramGetMeResponseDto {

    /** {@code true} si la llamada fue exitosa según Telegram. */
    @JsonProperty("ok")
    private Boolean ok;

    /** Información del bot asociado al token validado. */
    @JsonProperty("result")
    private BotInfoDto result;

    /** Mensaje de error de Telegram cuando {@code ok=false}. */
    @JsonProperty("description")
    private String description;

    /**
     * Datos del bot devueltos por {@code /getMe}.
     * Los campos desconocidos se ignoran para mantener compatibilidad
     * con futuras versiones de la API de Telegram.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BotInfoDto {

        /** ID único del bot en Telegram. */
        @JsonProperty("id")
        private Long id;

        /** Siempre {@code true} para bots. */
        @JsonProperty("is_bot")
        private Boolean isBot;

        /** Nombre visible del bot. */
        @JsonProperty("first_name")
        private String firstName;

        /** Username del bot (sin el {@code @}). */
        @JsonProperty("username")
        private String username;
    }
}

