package com.iclinic.iclinicbackend.modules.crm.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(Long id) {
        super("Conversación no encontrada: " + id);
    }
}

