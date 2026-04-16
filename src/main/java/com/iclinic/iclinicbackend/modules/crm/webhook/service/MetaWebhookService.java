package com.iclinic.iclinicbackend.modules.crm.webhook.service;

public interface MetaWebhookService {
    String verifyWebhook(String mode, String token, String challenge);
    void processIncomingPayload(String payload);
}

