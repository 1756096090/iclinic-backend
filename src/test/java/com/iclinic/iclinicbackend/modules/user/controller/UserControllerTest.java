package com.iclinic.iclinicbackend.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.modules.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Search Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("shouldSearchUsersWithDefaultLimit")
    void shouldSearchUsersWithDefaultLimit() throws Exception {
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1L)
                .fullName("Veronica Salazar")
                .email("veronica@clinic.com")
                .build();

        when(userService.searchByBranchIdAndText(1L, "ver", 20)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/users/search")
                        .param("branchId", "1")
                        .param("query", "ver")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].fullName").value("Veronica Salazar"));

        verify(userService).searchByBranchIdAndText(1L, "ver", 20);
    }

    @Test
    @DisplayName("shouldSearchUsersWithCustomLimit")
    void shouldSearchUsersWithCustomLimit() throws Exception {
        when(userService.searchByBranchIdAndText(2L, "ana", 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/search")
                        .param("branchId", "2")
                        .param("query", "ana")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(userService).searchByBranchIdAndText(eq(2L), eq("ana"), eq(5));
    }
}

