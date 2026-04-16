package com.iclinic.iclinicbackend.modules.crm.exception;

import com.iclinic.iclinicbackend.shared.enums.ChannelType;

/**
 * Lanzada cuando no existe una conexión de canal activa/verificada
 * para la empresa y tipo de canal indicados.
 * Reemplaza RuntimeException genérica en ChannelInboundMessageProcessor.
 */
public class ChannelConnectionActiveNotFoundException extends RuntimeException {

    public ChannelConnectionActiveNotFoundException(Long companyId, ChannelType channelType) {
        super("No active channel connection found for companyId=" + companyId
              + " and channelType=" + channelType);
    }
}

