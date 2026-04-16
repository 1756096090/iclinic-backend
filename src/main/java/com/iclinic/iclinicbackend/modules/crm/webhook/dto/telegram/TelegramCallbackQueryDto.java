package com.iclinic.iclinicbackend.modules.crm.webhook.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta a un botón inline (inline keyboard callback).
 * Ver: https://core.telegram.org/bots/api#callbackquery
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramCallbackQueryDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("from")
    private TelegramUserDto from;

    /** Mensaje que contiene el teclado inline que originó el callback. */
    @JsonProperty("message")
    private TelegramMessageDto message;

    /** Datos asociados al botón presionado. */
    @JsonProperty("data")
    private String data;
}

