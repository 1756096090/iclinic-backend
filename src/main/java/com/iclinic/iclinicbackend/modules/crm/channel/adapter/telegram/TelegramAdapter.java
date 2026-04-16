package com.iclinic.iclinicbackend.modules.crm.channel.adapter.telegram;

import com.iclinic.iclinicbackend.modules.crm.channel.adapter.MessagingChannelAdapter;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.security.SecretEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramAdapter implements MessagingChannelAdapter {

    private final TelegramClient telegramClient;
    private final SecretEncryptionService secretEncryptionService;

    @Override
    public ChannelType supports() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public String sendText(ChannelConnection connection, String to, String message) {
        log.info("Sending Telegram message to chatId={}", to);

        try {
            String token = decryptToken(connection.getAccessTokenEncrypted());
            return telegramClient.sendMessage(token, to, message);
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            throw new RuntimeException("Error sending Telegram message", e);
        }
    }

    private String decryptToken(String encryptedToken) {
        return secretEncryptionService.decrypt(encryptedToken);
    }
}
