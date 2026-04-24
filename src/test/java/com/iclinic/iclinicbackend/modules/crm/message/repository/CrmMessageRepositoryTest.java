package com.iclinic.iclinicbackend.modules.crm.message.repository;

import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelConnectionRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactRepository;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.modules.crm.conversation.repository.ConversationRepository;
import com.iclinic.iclinicbackend.modules.crm.message.entity.CrmMessage;
import com.iclinic.iclinicbackend.shared.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("CrmMessageRepository Tests")
class CrmMessageRepositoryTest {

    @Autowired
    private CrmMessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChannelConnectionRepository channelRepository;

    @Autowired
    private CrmContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Conversation conversation;
    private ChannelConnection channel;

    @BeforeEach
    void setUp() {
        // Create company
        EcuadorianCompany company = new EcuadorianCompany("Test Clinic", "1234567890001");
        company = companyRepository.save(company);

        // Create contact
        CrmContact contact = CrmContact.builder()
                .fullName("Juan Pérez")
                .phone("+593987654321")
                .company(company)
                .build();
        contact = contactRepository.save(contact);

        // Create channel
        channel = ChannelConnection.builder()
                .company(company)
                .channelType(ChannelType.WHATSAPP)
                .provider(ChannelProvider.META)
                .accessTokenEncrypted("token")
                .status(ChannelConnectionStatus.ACTIVE)
                .build();
        channel = channelRepository.save(channel);

        // Create conversation
        conversation = Conversation.builder()
                .contact(contact)
                .channelConnection(channel)
                .status(ConversationStatus.OPEN)
                .build();
        conversation = conversationRepository.save(conversation);
    }

    @Test
    @DisplayName("Should find messages by conversation ordered by creation date")
    void testFindByConversationIdOrderByCreatedAtAsc() {
        // Create multiple messages
        CrmMessage msg1 = CrmMessage.builder()
                .conversation(conversation)
                .channelType(ChannelType.WHATSAPP)
                .content("First message")
                .direction(MessageDirection.INBOUND)
                .status(MessageStatus.RECEIVED)
                .build();
        messageRepository.save(msg1);

        // Add a small delay to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        CrmMessage msg2 = CrmMessage.builder()
                .conversation(conversation)
                .channelType(ChannelType.WHATSAPP)
                .content("Second message")
                .direction(MessageDirection.OUTBOUND)
                .status(MessageStatus.SENT)
                .build();
        messageRepository.save(msg2);

        // Retrieve messages
        List<CrmMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("First message");
        assertThat(messages.get(1).getContent()).isEqualTo("Second message");
    }

    @Test
    @DisplayName("Should find message by channel type and external message ID")
    void testFindByChannelTypeAndExternalMessageId() {
        CrmMessage message = CrmMessage.builder()
                .conversation(conversation)
                .channelType(ChannelType.TELEGRAM)
                .externalMessageId("tg-msg-12345")
                .content("Telegram message")
                .direction(MessageDirection.INBOUND)
                .status(MessageStatus.RECEIVED)
                .build();
        messageRepository.save(message);

        Optional<CrmMessage> found = messageRepository
                .findByChannelTypeAndExternalMessageId(ChannelType.TELEGRAM, "tg-msg-12345");

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Telegram message");
    }

    @Test
    @DisplayName("Should return empty when message not found by channel type and ID")
    void testFindByChannelTypeAndExternalMessageIdNotFound() {
        Optional<CrmMessage> found = messageRepository
                .findByChannelTypeAndExternalMessageId(ChannelType.TELEGRAM, "non-existent-id");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for conversation with no messages")
    void testFindByConversationIdEmptyList() {
        List<CrmMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("Should not return messages from other conversations")
    void testFindDoesNotReturnMessagesFromOtherConversations() {
        // Create another conversation
        CrmContact contact2 = CrmContact.builder()
                .fullName("María García")
                .phone("+593991234567")
                .company(channel.getCompany())
                .build();
        contact2 = contactRepository.save(contact2);

        Conversation conversation2 = Conversation.builder()
                .contact(contact2)
                .channelConnection(channel)
                .status(ConversationStatus.OPEN)
                .build();
        conversation2 = conversationRepository.save(conversation2);

        // Add messages to first conversation
        CrmMessage msg1 = CrmMessage.builder()
                .conversation(conversation)
                .channelType(ChannelType.WHATSAPP)
                .content("Message in first conversation")
                .direction(MessageDirection.INBOUND)
                .status(MessageStatus.RECEIVED)
                .build();
        messageRepository.save(msg1);

        // Add messages to second conversation
        CrmMessage msg2 = CrmMessage.builder()
                .conversation(conversation2)
                .channelType(ChannelType.WHATSAPP)
                .content("Message in second conversation")
                .direction(MessageDirection.INBOUND)
                .status(MessageStatus.RECEIVED)
                .build();
        messageRepository.save(msg2);

        // Query first conversation
        List<CrmMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Message in first conversation");
    }
}

