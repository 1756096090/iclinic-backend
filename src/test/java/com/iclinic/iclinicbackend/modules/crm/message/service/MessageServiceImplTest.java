package com.iclinic.iclinicbackend.modules.crm.message.service;

import com.iclinic.iclinicbackend.modules.crm.channel.adapter.MessagingChannelAdapter;
import com.iclinic.iclinicbackend.modules.crm.channel.adapter.MessagingChannelAdapterRegistry;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelUserLinkRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.service.CrmContactService;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.conversation.service.ConversationService;
import com.iclinic.iclinicbackend.modules.crm.message.dto.SendMessageRequestDto;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.modules.crm.message.repository.CrmMessageRepository;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.InboundWebhookMessageDto;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.iclinic.iclinicbackend.modules.crm.exception.ChannelConnectionNotFoundException;
import com.iclinic.iclinicbackend.modules.crm.exception.ConversationNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Tests")
class MessageServiceImplTest {

    @Mock private CrmMessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChannelConnectionRepository channelConnectionRepository;
    @Mock private ChannelUserLinkRepository channelUserLinkRepository;
    @Mock private CrmContactService contactService;
    @Mock private ConversationService conversationService;
    @Mock private MessagingChannelAdapterRegistry adapterRegistry;
    @Mock private MessagingChannelAdapter whatsAppAdapter;

    @InjectMocks
    private MessageServiceImpl messageService;

    private CrmContact contact;
    private ChannelConnection connection;
    private Conversation conversation;
    private EcuadorianUser user;
    private ChannelUserLink channelUserLink;

    @BeforeEach
    void setUp() {
        contact = CrmContact.builder()
                .id(1L).phone("+593987654321").fullName("Juan Pérez").build();

        connection = ChannelConnection.builder()
                .id(1L).channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .status(ChannelConnectionStatus.ACTIVE)
                .externalAccountId("acc-123")
                .accessTokenEncrypted("token-enc")
                .webhookVerifyToken("verify-token")
                .build();

        channelUserLink = ChannelUserLink.builder()
                .id(1L)
                .contact(contact)
                .channelType(ChannelType.WHATSAPP)
                .externalUserId("wa-user-123")
                .externalChatId("12345678901")
                .username("juan_perez")
                .displayName("Juan Pérez")
                .build();

        conversation = Conversation.builder()
                .id(10L).contact(contact).channelConnection(connection)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();

        user = new EcuadorianUser();
        user.setId(5L);
        user.setFirstName("Dr");
        user.setLastName("House");
    }

    @Test
    @DisplayName("shouldSendOutboundTextMessageSuccessfully")
    void testSendOutboundMessage() {
        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setConversationId(10L);
        dto.setSentByUserId(5L);
        dto.setContent("Hola, ¿cómo está?");

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(channelUserLinkRepository.findByContactAndChannelType(contact, ChannelType.WHATSAPP))
                .thenReturn(Optional.of(channelUserLink));
        when(adapterRegistry.get(ChannelType.WHATSAPP)).thenReturn(whatsAppAdapter);
        when(whatsAppAdapter.sendText(eq(connection), eq("12345678901"), eq("Hola, ¿cómo está?")))
                .thenReturn("wa-msg-12345");
        when(messageRepository.save(any(CrmMessage.class))).thenAnswer(inv -> {
            CrmMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
        when(conversationRepository.save(any())).thenReturn(conversation);

        CrmMessage result = messageService.sendText(dto);

        assertNotNull(result);
        assertEquals(MessageDirection.OUTBOUND, result.getDirection());
        assertEquals(MessageStatus.SENT, result.getStatus());
        assertEquals("wa-msg-12345", result.getExternalMessageId());
        assertEquals("Hola, ¿cómo está?", result.getContent());
        verify(messageRepository).save(any(CrmMessage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenConversationNotFoundForSend")
    void testThrowWhenConversationNotFound() {
        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setConversationId(999L);
        dto.setSentByUserId(5L);
        dto.setContent("test");

        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> messageService.sendText(dto));
    }

    @Test
    @DisplayName("shouldProcessInboundMessageSuccessfully")
    void testProcessInboundMessage() {
        InboundWebhookMessageDto dto = InboundWebhookMessageDto.builder()
                .channelType(ChannelType.WHATSAPP)
                .companyId(1L)
                .contactName("Juan Pérez")
                .contactPhone("+593987654321")
                .externalMessageId("ext-msg-001")
                .content("Quiero una cita")
                .build();

        when(channelConnectionRepository.findByCompanyIdAndChannelTypeAndStatus(
                1L, ChannelType.WHATSAPP, ChannelConnectionStatus.ACTIVE))
                .thenReturn(Optional.of(connection));
        when(contactService.resolveContact(eq(connection), any()))
                .thenReturn(contact);
        when(conversationService.findOrCreateOpenConversation(contact, connection))
                .thenReturn(conversation);
        when(messageRepository.save(any(CrmMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any())).thenReturn(conversation);

        assertDoesNotThrow(() -> messageService.processInboundMessage(dto));

        verify(messageRepository).save(argThat(m ->
                m.getDirection() == MessageDirection.INBOUND
                && "ext-msg-001".equals(m.getExternalMessageId())
                && "Quiero una cita".equals(m.getContent())
        ));
    }

    @Test
    @DisplayName("shouldThrowWhenNoActiveChannelForInbound")
    void testThrowWhenNoActiveChannel() {
        InboundWebhookMessageDto dto = InboundWebhookMessageDto.builder()
                .channelType(ChannelType.WHATSAPP).companyId(999L)
                .contactPhone("+593000000000").content("test").build();

        when(channelConnectionRepository.findByCompanyIdAndChannelTypeAndStatus(
                999L, ChannelType.WHATSAPP, ChannelConnectionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ChannelConnectionNotFoundException.class, () ->
                messageService.processInboundMessage(dto));
    }

    @Test
    @DisplayName("shouldReturnMessagesByConversation")
    void testFindByConversation() {
        CrmMessage msg = CrmMessage.builder().id(1L).content("Hola").build();
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(msg));

        List<CrmMessage> result = messageService.findByConversation(10L);

        assertEquals(1, result.size());
        assertEquals("Hola", result.get(0).getContent());
    }
}
