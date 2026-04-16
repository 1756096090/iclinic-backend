package com.iclinic.iclinicbackend.modules.crm.exception;

public class DuplicateMessageException extends RuntimeException {
    public DuplicateMessageException(String externalMessageId) {
        super("Message with externalMessageId=" + externalMessageId + " has already been processed");
    }
}