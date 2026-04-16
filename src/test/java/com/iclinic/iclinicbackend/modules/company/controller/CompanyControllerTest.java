package com.iclinic.iclinicbackend.modules.company.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.company.dto.CreateEcuadorianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.EcuadorianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
@DisplayName("CompanyController Tests")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    private CreateEcuadorianCompanyRequestDto ecuadorianRequestDto;
    private EcuadorianCompanyResponseDto ecuadorianResponseDto;

    @BeforeEach
    void setUp() {
        // Setup Request DTO
        ecuadorianRequestDto = new CreateEcuadorianCompanyRequestDto();
        ecuadorianRequestDto.setName("Clinica Dental XYZ");
        ecuadorianRequestDto.setRuc("1712345678901");
        ecuadorianRequestDto.setBranches(new ArrayList<>());

        // Setup Response DTO
        ecuadorianResponseDto = new EcuadorianCompanyResponseDto();
        ecuadorianResponseDto.setId(1L);
        ecuadorianResponseDto.setName("Clinica Dental XYZ");
        ecuadorianResponseDto.setRuc("1712345678901");
    }

    @Test
    @DisplayName("shouldCreateEcuadorianCompanyAndReturnCreated")
    void testCreateEcuadorianCompanyAndReturnCreated() throws Exception {
        // Arrange
        when(companyService.createEcuadorianCompany(any(CreateEcuadorianCompanyRequestDto.class)))
                .thenReturn(ecuadorianResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/companies/ecuadorian")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ecuadorianRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Clinica Dental XYZ"))
                .andExpect(jsonPath("$.ruc").value("1712345678901"));

        verify(companyService, times(1)).createEcuadorianCompany(any(CreateEcuadorianCompanyRequestDto.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenNameIsEmpty")
    void testReturnBadRequestWhenNameIsEmpty() throws Exception {
        // Arrange
        CreateEcuadorianCompanyRequestDto invalidDto = new CreateEcuadorianCompanyRequestDto();
        invalidDto.setName(""); // Empty name
        invalidDto.setRuc("1712345678901");

        // Act & Assert
        mockMvc.perform(post("/api/v1/companies/ecuadorian")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validación fallida"));

        verify(companyService, never()).createEcuadorianCompany(any());
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenRucIsInvalid")
    void testReturnBadRequestWhenRucIsInvalid() throws Exception {
        // Arrange
        CreateEcuadorianCompanyRequestDto invalidDto = new CreateEcuadorianCompanyRequestDto();
        invalidDto.setName("Clinica Dental XYZ");
        invalidDto.setRuc("123"); // Invalid RUC (too short)

        // Act & Assert
        mockMvc.perform(post("/api/v1/companies/ecuadorian")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validación fallida"));

        verify(companyService, never()).createEcuadorianCompany(any());
    }

    @Test
    @DisplayName("shouldGetAllCompanies")
    void testGetAllCompanies() throws Exception {
        // Arrange
        when(companyService.findAll()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.isA(java.util.List.class)));

        verify(companyService, times(1)).findAll();
    }

    @Test
    @DisplayName("shouldGetCompanyById")
    void testGetCompanyById() throws Exception {
        // Arrange
        EcuadorianCompany company = new EcuadorianCompany();
        company.setId(1L);
        company.setName("Clinica Dental XYZ");
        company.setRuc("1712345678901");

        when(companyService.findById(1L)).thenReturn(company);

        // Act & Assert
        mockMvc.perform(get("/api/v1/companies/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Clinica Dental XYZ"));

        verify(companyService, times(1)).findById(1L);
    }

    @Test
    @DisplayName("shouldDeleteCompanyAndReturnNoContent")
    void testDeleteCompanyAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(companyService).deleteById(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/companies/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(companyService, times(1)).deleteById(1L);
    }
}


