package com.iclinic.iclinicbackend.shared.enums;

/**
 * Naturaleza del sujeto de Keycloak que hay detrás de una fila de {@code users}.
 * <p>
 * La distinción no es cosmética: una cuenta de servicio nunca puede leer historia
 * clínica, y esa prohibición se comprueba por este campo además de por los scopes,
 * para que una casilla mal marcada en la consola de Keycloak no pueda habilitarla.
 */
public enum SubjectType {
    /** Persona con credencial propia. */
    HUMAN,
    /** Integración (agente de WhatsApp, planificador, proceso por lotes). */
    SERVICE_ACCOUNT
}
