package com.iclinic.iclinicbackend.shared.enums;

public enum ChannelConnectionStatus {
    /** Conexión creada pero sin verificar aún. */
    PENDING,
    /** Webhook verificado por el proveedor; lista para recibir mensajes. */
    VERIFIED,
    /** Activada manualmente por un administrador. */
    ACTIVE,
    /** Deshabilitada. */
    INACTIVE
}