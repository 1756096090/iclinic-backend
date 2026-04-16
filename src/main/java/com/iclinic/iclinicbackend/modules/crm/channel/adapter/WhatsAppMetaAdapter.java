package com.iclinic.iclinicbackend.modules.crm.channel.adapter;

import com.iclinic.iclinicbackend.modules.crm.channel.adapter.meta.MetaWhatsAppClient;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.security.SecretEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMetaAdapter implements MessagingChannelAdapter {

    private final MetaWhatsAppClient metaWhatsAppClient;
    private final SecretEncryptionService secretEncryptionService;

    @Override
    public ChannelType supports() {
        return ChannelType.WHATSAPP;
    }

    @Override
    public String sendText(ChannelConnection connection, String to, String message) {
        String accessToken = secretEncryptionService.decrypt(connection.getAccessTokenEncrypted());
        String phoneNumberId = connection.getExternalPhoneNumberId();

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new IllegalStateException(
                    "La conexión de canal id=" + connection.getId() + " no tiene externalPhoneNumberId configurado");
        }

        return metaWhatsAppClient.sendTextMessage(accessToken, phoneNumberId, to, message);
    }
}

