package com.iclinic.iclinicbackend.modules.crm.exception;

public class ChannelConnectionNotFoundException extends RuntimeException {
    public ChannelConnectionNotFoundException(Long id) {
        super("Canal no encontrado: " + id);
    }

    public ChannelConnectionNotFoundException(String message) {
        super(message);
    }
}

