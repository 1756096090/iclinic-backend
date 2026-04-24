package com.iclinic.iclinicbackend.modules.crm.channel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.ChannelConnectionResponseDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.CreateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.UpdateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.service.ChannelConnectionService;
import com.iclinic.iclinicbackend.shared.enums.ChannelConnectionStatus;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.enums.ChannelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ChannelConnectionController Tests")
class ChannelConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChannelConnectionService channelConnectionService;

    private ChannelConnectionResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = ChannelConnectionResponseDto.builder()
                .id(1L)
                .channelType(ChannelType.TELEGRAM)
                .provider(ChannelProvider.TELEGRAM)
                .status(ChannelConnectionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should get all channels successfully")
    void testGetAll() throws Exception {
        List<ChannelConnectionResponseDto> channels = List.of(responseDto);
        when(channelConnectionService.getAll()).thenReturn(channels);

        mockMvc.perform(get("/api/v1/crm/channels")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].channelType").value("TELEGRAM"));

        verify(channelConnectionService).getAll();
    }

    @Test
    @DisplayName("Should create channel successfully")
    void testCreateChannel() throws Exception {
        CreateChannelConnectionRequestDto createDto = new CreateChannelConnectionRequestDto();
        createDto.setCompanyId(1L);
        createDto.setChannelType(ChannelType.TELEGRAM);
        createDto.setProvider(ChannelProvider.TELEGRAM);
        createDto.setAccessToken("test-token-123");
        createDto.setWebhookVerifyToken("verify-token");

        when(channelConnectionService.create(any(CreateChannelConnectionRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/crm/channels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.channelType").value("TELEGRAM"));

        verify(channelConnectionService).create(any(CreateChannelConnectionRequestDto.class));
    }

    @Test
    @DisplayName("Should update channel successfully")
    void testUpdateChannel() throws Exception {
        UpdateChannelConnectionRequestDto updateDto = new UpdateChannelConnectionRequestDto();
        updateDto.setAccessToken("updated-token");

        when(channelConnectionService.update(eq(1L), any(UpdateChannelConnectionRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/crm/channels/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(channelConnectionService).update(eq(1L), any(UpdateChannelConnectionRequestDto.class));
    }

    @Test
    @DisplayName("Should activate channel successfully")
    void testActivateChannel() throws Exception {
        ChannelConnectionResponseDto activeDto = ChannelConnectionResponseDto.builder()
                .id(1L)
                .status(ChannelConnectionStatus.ACTIVE)
                .build();

        when(channelConnectionService.activate(1L)).thenReturn(activeDto);

        mockMvc.perform(patch("/api/v1/crm/channels/1/activate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(channelConnectionService).activate(1L);
    }

    @Test
    @DisplayName("Should deactivate channel successfully")
    void testDeactivateChannel() throws Exception {
        ChannelConnectionResponseDto inactiveDto = ChannelConnectionResponseDto.builder()
                .id(1L)
                .status(ChannelConnectionStatus.INACTIVE)
                .build();

        when(channelConnectionService.deactivate(1L)).thenReturn(inactiveDto);

        mockMvc.perform(patch("/api/v1/crm/channels/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(channelConnectionService).deactivate(1L);
    }

    @Test
    @DisplayName("Should delete channel (soft delete)")
    void testDeleteChannel() throws Exception {
        doNothing().when(channelConnectionService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/crm/channels/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(channelConnectionService).deleteById(1L);
    }

    @Test
    @DisplayName("Should find channels by company")
    void testFindByCompany() throws Exception {
        List<ChannelConnectionResponseDto> channels = List.of(responseDto);
        when(channelConnectionService.findByCompany(1L)).thenReturn(channels);

        mockMvc.perform(get("/api/v1/crm/channels/company/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(channelConnectionService).findByCompany(1L);
    }
}

