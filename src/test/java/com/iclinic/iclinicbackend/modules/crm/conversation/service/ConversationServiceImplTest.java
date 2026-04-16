package com.iclinic.iclinicbackend.modules.crm.conversation.service;

import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.exception.ConversationNotFoundException;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
class ConversationServiceImplTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private CrmContact contact;
    private ChannelConnection channelConnection;
    private Conversation openConversation;
    private EcuadorianCompany company;

    @BeforeEach
    void setUp() {
        company = new EcuadorianCompany("Clinica XYZ", "1712345678901");
        company.setId(1L);

        contact = CrmContact.builder().id(1L).phone("+593987654321").company(company).build();

        channelConnection = ChannelConnection.builder()
                .id(1L)
                .company(company)
                .channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .status(ChannelConnectionStatus.ACTIVE)
                .build();

        openConversation = Conversation.builder()
                .id(10L)
                .contact(contact)
                .channelConnection(channelConnection)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("shouldReturnExistingOpenConversation")
    void testReturnExistingOpenConversation() {
        when(conversationRepository.findFirstByContactIdAndStatusOrderByCreatedAtDesc(
                1L, ConversationStatus.OPEN))
                .thenReturn(Optional.of(openConversation));

        Conversation result = conversationService.findOrCreateOpenConversation(contact, channelConnection);

        assertEquals(10L, result.getId());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateNewConversationWhenNoneOpen")
    void testCreateNewConversationWhenNoneOpen() {
        when(conversationRepository.findFirstByContactIdAndStatusOrderByCreatedAtDesc(
                1L, ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        Conversation result = conversationService.findOrCreateOpenConversation(contact, channelConnection);

        assertNotNull(result);
        assertEquals(ConversationStatus.OPEN, result.getStatus());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("shouldAssignUserToConversation")
    void testAssignUserToConversation() {
        EcuadorianUser user = new EcuadorianUser();
        user.setId(5L);

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(openConversation));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        Conversation result = conversationService.assign(10L, 5L);

        assertNotNull(result.getAssignedUser());
        assertEquals(5L, result.getAssignedUser().getId());
    }

    @Test
    @DisplayName("shouldThrowWhenConversationNotFoundForAssign")
    void testThrowWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () ->
                conversationService.assign(999L, 5L));
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundForAssign")
    void testThrowWhenUserNotFound() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(openConversation));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                conversationService.assign(10L, 999L));
    }
}

