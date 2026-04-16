package com.iclinic.iclinicbackend.modules.crm.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.message.service.MessageService;
import com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetaWebhookService Tests")
class MetaWebhookServiceImplTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private MessageService messageService;
    @Mock private ChannelConnectionRepository channelConnectionRepository;

    @InjectMocks
    private MetaWebhookServiceImpl metaWebhookService;

    private ChannelConnection activeConnection;

    @BeforeEach
    void setUp() {
        activeConnection = ChannelConnection.builder()
                .id(1L)
                .channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .status(ChannelConnectionStatus.ACTIVE)
                .webhookVerifyToken("my-secret-token")
                .accessTokenEncrypted("token-enc")
                .externalAccountId("acc-123")
                .externalPhoneNumberId("phone-123")
                .build();
    }

    @Test
    @DisplayName("shouldVerifyWebhookWithValidToken")
    void testVerifyWithValidToken() {
        when(channelConnectionRepository.findByWebhookVerifyToken("my-secret-token"))
                .thenReturn(Optional.of(activeConnection));

        String result = metaWebhookService.verifyWebhook("subscribe", "my-secret-token", "challenge-abc");

        assertEquals("challenge-abc", result);
    }

    @Test
    @DisplayName("shouldThrowWhenModeIsNotSubscribe")
    void testThrowWhenInvalidMode() {
        assertThrows(InvalidChannelConfigurationException.class, () ->
                metaWebhookService.verifyWebhook("unsubscribe", "my-secret-token", "challenge"));
    }

    @Test
    @DisplayName("shouldThrowWhenTokenNotFound")
    void testThrowWhenTokenNotFound() {
        when(channelConnectionRepository.findByWebhookVerifyToken("wrong-token"))
                .thenReturn(Optional.empty());

        assertThrows(ChannelConnectionNotFoundException.class, () ->
                metaWebhookService.verifyWebhook("subscribe", "wrong-token", "challenge"));
    }

    @Test
    @DisplayName("shouldProcessValidWhatsAppPayload")
    void testProcessValidPayload() {
        String payload = """
                {
                  "companyId": 1,
                  "entry": [{
                    "changes": [{
                      "value": {
                        "metadata": { "phone_number_id": "phone-123" },
                        "messages": [{ "from": "+593987654321", "id": "ext-msg-001", "text": { "body": "Hola" } }],
                        "contacts": [{ "profile": { "name": "Juan" } }]
                      }
                    }]
                  }]
                }
                """;

        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);
        activeConnection.setCompany(company);

        when(channelConnectionRepository.findByExternalPhoneNumberIdAndChannelTypeAndStatusInWithCompany(
                eq("phone-123"), eq(ChannelType.WHATSAPP), any()))
                .thenReturn(Optional.of(activeConnection));
        doNothing().when(messageService).processInboundMessage(any());

        assertDoesNotThrow(() -> metaWebhookService.processIncomingPayload(payload));
        verify(messageService).processInboundMessage(any());
    }

    @Test
    @DisplayName("shouldThrowWhenPayloadIsInvalidJson")
    void testThrowWhenInvalidJson() {
        assertThrows(InvalidChannelConfigurationException.class, () ->
                metaWebhookService.processIncomingPayload("not-json-{{{"));
    }
}
