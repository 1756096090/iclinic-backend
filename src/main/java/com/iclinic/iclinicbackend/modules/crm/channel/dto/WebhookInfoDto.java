package com.iclinic.iclinicbackend.modules.crm.channel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Información del webhook registrado en Telegram para un canal,
 * tal como la devuelve {@code /getWebhookInfo}.
 */
@Data
@Builder
public class WebhookInfoDto {

    /** URL actualmente registrada en Telegram. Vacía si no hay webhook. */
    private String url;

    /** Si Telegram tiene un certificado personalizado para este webhook. */
    private boolean hasCustomCertificate;

    /** Cantidad de updates pendientes de entrega. */
    private int pendingUpdateCount;

    /** Fecha de la última vez que Telegram intentó enviar un update (epoch segundos). */
    private Long lastErrorDate;

    /** Descripción del último error si hubo fallo. */
    private String lastErrorMessage;

    /** Número máximo de conexiones simultáneas. */
    private int maxConnections;

    /** Indica si la URL registrada coincide con la URL esperada del servidor. */
    private boolean urlMatches;

    /** URL esperada según la configuración del servidor. */
    private String expectedUrl;
}
