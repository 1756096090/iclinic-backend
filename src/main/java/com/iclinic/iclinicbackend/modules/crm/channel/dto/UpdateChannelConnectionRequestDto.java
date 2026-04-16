package com.iclinic.iclinicbackend.modules.crm.channel.dto;

import lombok.Data;

/**
 * DTO para actualizar una conexión de canal existente.
 * Todos los campos son opcionales; solo se actualizan los que se envíen no-nulos/no-vacíos.
 * Si se envía {@code webhookBaseUrl}, se re-registra el webhook en Telegram.
 */
@Data
public class UpdateChannelConnectionRequestDto {

    /** Nuevo token de acceso. Si se omite, se conserva el existente. */
    private String accessToken;

    /** Nuevo token de verificación de webhook. Si se omite, se conserva el existente. */
    private String webhookVerifyToken;

    /** URL base del webhook. Si se proporciona, re-registra el webhook en Telegram y activa el canal. */
    private String webhookBaseUrl;
}
