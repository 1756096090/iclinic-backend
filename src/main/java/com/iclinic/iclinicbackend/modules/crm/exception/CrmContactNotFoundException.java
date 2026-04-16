package com.iclinic.iclinicbackend.modules.crm.exception;

public class CrmContactNotFoundException extends RuntimeException {
    public CrmContactNotFoundException(Long id) {
        super("Contacto CRM no encontrado: " + id);
    }

    public CrmContactNotFoundException(String message) {
        super(message);
    }
}

