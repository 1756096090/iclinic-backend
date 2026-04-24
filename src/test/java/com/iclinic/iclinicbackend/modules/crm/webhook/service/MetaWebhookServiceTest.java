package com.iclinic.iclinicbackend.modules.crm.webhook.service;

import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.message.service.MessageService;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.InboundWebhookMessageDto;
import com.iclinic.iclinicbackend.shared.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetaWebhookService Tests")
class MetaWebhookServiceImplTest {

    @Mock private ChannelConnectionRepository channelConnectionRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private MetaWebhookServiceImpl metaWebhookService;

    private EcuadorianCompany company;
    private ChannelConnection channelConnection;

    @BeforeEach
    void setUp() {
        company = new EcuadorianCompany("Test Clinic", "1234567890001");
        company.setId(1L);

        channelConnection = ChannelConnection.builder()
                .id(1L)
                .company(company)
                .channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .externalPhoneNumberId("123456789")
                .status(ChannelConnectionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should process valid WhatsApp webhook event")
    void testProcessValidWebhookEvent() {
        InboundWebhookMessageDto messageDto = InboundWebhookMessageDto.builder()
                .channelType(ChannelType.WHATSAPP)
                .companyId(1L)
                .contactName("Juan Pérez")
                .contactPhone("+593987654321")
                .externalMessageId("wa-msg-001")
                .content("Hola, necesito ayuda")
                .build();

        when(channelConnectionRepository.findByExternalPhoneNumberIdAndChannelTypeAndStatusInWithCompany(
                "123456789", ChannelType.WHATSAPP, Set.of(ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED)))
                .thenReturn(Optional.of(channelConnection));
        doNothing().when(messageService).processInboundMessage(any(InboundWebhookMessageDto.class));

        assertDoesNotThrow(() -> metaWebhookService.processIncomingPayload("""
                {
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "+593987654321",
                          "id": "wa-msg-001",
                          "text": { "body": "Hola, necesito ayuda" }
                        }],
                        "contacts": [{
                          "profile": { "name": "Juan Pérez" }
                        }],
                        "metadata": { "phone_number_id": "123456789" }
                      }
                    }]
                  }]
                }
                """));
    }

    @Test
    @DisplayName("Should handle missing channel gracefully")
    void testHandleMissingChannel() {
        when(channelConnectionRepository.findByExternalPhoneNumberIdAndChannelTypeAndStatusInWithCompany(
                "999999999", ChannelType.WHATSAPP, Set.of(ChannelConnectionStatus.ACTIVE, ChannelConnectionStatus.VERIFIED)))
                .thenReturn(Optional.empty());

        // Should handle gracefully without throwing
        assertDoesNotThrow(() -> metaWebhookService.processIncomingPayload("""
                {
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "+593000000000",
                          "id": "wa-msg-999",
                          "text": { "body": "Test message" }
                        }],
                        "contacts": [{
                          "profile": { "name": "Test User" }
                        }],
                        "metadata": { "phone_number_id": "999999999" }
                      }
                    }]
                  }]
                }
                """));

        verify(messageService, never()).processInboundMessage(any());
    }
}

