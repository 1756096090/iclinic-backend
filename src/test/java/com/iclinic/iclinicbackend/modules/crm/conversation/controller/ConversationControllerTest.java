package com.iclinic.iclinicbackend.modules.crm.conversation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto;
import com.iclinic.iclinicbackend.modules.crm.conversation.service.ConversationService;
import com.iclinic.iclinicbackend.shared.enums.ConversationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConversationController Tests")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    private ConversationResponseDto conversationDto;

    @BeforeEach
    void setUp() {
        conversationDto = ConversationResponseDto.builder()
                .id(1L)
                .contactId(1L)
                .channelConnectionId(1L)
                .status(ConversationStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should get all conversations")
    void testGetAllConversations() throws Exception {
        List<ConversationResponseDto> conversations = List.of(conversationDto);
        when(conversationService.findAll()).thenReturn(null); // Note: may need to adjust based on actual API

        mockMvc.perform(get("/api/v1/crm/conversations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conversationService, atLeastOnce()).findAll();
    }

    @Test
    @DisplayName("Should find conversations by company")
    void testFindByCompany() throws Exception {
        com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation conv = 
            com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation.builder()
                .id(1L)
                .build();
        when(conversationService.findByCompany(1L)).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/crm/conversations/company/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conversationService).findByCompany(1L);
    }

    @Test
    @DisplayName("Should find conversations by branch")
    void testFindByBranch() throws Exception {
        com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation conv = 
            com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation.builder()
                .id(1L)
                .build();
        when(conversationService.findByBranch(1L)).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/crm/conversations/branch/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conversationService).findByBranch(1L);
    }

    @Test
    @DisplayName("Should assign conversation to user")
    void testAssignConversation() throws Exception {
        com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation assigned = 
            com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation.builder()
                .id(1L)
                .status(ConversationStatus.OPEN)
                .build();

        when(conversationService.assign(1L, 5L)).thenReturn(assigned);

        mockMvc.perform(patch("/api/v1/crm/conversations/1/assign/5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(conversationService).assign(1L, 5L);
    }
}

