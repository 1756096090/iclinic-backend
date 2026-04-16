package com.iclinic.iclinicbackend.modules.crm.exception;

public class TelegramWebhookException extends RuntimeException {

    public TelegramWebhookException(String message) {
        super(message);
    }

    public TelegramWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
