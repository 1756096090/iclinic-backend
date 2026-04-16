package com.iclinic.iclinicbackend.modules.branch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.branch.dto.ClinicBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.HospitalBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.service.BranchService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests del controlador de sucursales con @WebMvcTest
@WebMvcTest(BranchController.class)
@DisplayName("BranchController Tests")
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BranchService branchService;

    private CreateClinicBranchRequestDto clinicRequestDto;
    private CreateHospitalBranchRequestDto hospitalRequestDto;
    private ClinicBranchResponseDto clinicResponseDto;
    private HospitalBranchResponseDto hospitalResponseDto;

    @BeforeEach
    void setUp() {
        // Arrange - Clinic
        clinicRequestDto = new CreateClinicBranchRequestDto();
        clinicRequestDto.setName("Sucursal Centro");
        clinicRequestDto.setAddress("Calle Principal 123");
        clinicRequestDto.setHasLaboratory(true);

        clinicResponseDto = new ClinicBranchResponseDto();
        clinicResponseDto.setId(1L);
        clinicResponseDto.setName("Sucursal Centro");
        clinicResponseDto.setAddress("Calle Principal 123");
        clinicResponseDto.setHasLaboratory(true);

        // Arrange - Hospital
        hospitalRequestDto = new CreateHospitalBranchRequestDto();
        hospitalRequestDto.setName("Hospital Central");
        hospitalRequestDto.setAddress("Av. Hospital 456");
        hospitalRequestDto.setBedCapacity(50);

        hospitalResponseDto = new HospitalBranchResponseDto();
        hospitalResponseDto.setId(2L);
        hospitalResponseDto.setName("Hospital Central");
        hospitalResponseDto.setAddress("Av. Hospital 456");
        hospitalResponseDto.setBedCapacity(50);
    }

    @Test
    @DisplayName("shouldCreateClinicBranchAndReturnCreated")
    void testCreateClinicBranchAndReturnCreated() throws Exception {
        // Arrange
        when(branchService.createClinicBranch(eq(1L), any(CreateClinicBranchRequestDto.class)))
                .thenReturn(clinicResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/branches/clinic/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clinicRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Sucursal Centro"))
                .andExpect(jsonPath("$.hasLaboratory").value(true));

        verify(branchService, times(1)).createClinicBranch(eq(1L), any(CreateClinicBranchRequestDto.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenClinicBranchNameIsEmpty")
    void testReturnBadRequestWhenClinicBranchNameIsEmpty() throws Exception {
        // Arrange
        CreateClinicBranchRequestDto invalidDto = new CreateClinicBranchRequestDto();
        invalidDto.setName("");
        invalidDto.setAddress("Calle Principal 123");
        invalidDto.setHasLaboratory(true);

        // Act & Assert
        mockMvc.perform(post("/api/v1/branches/clinic/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validación fallida"));

        verify(branchService, never()).createClinicBranch(any(), any());
    }

    @Test
    @DisplayName("shouldCreateHospitalBranchAndReturnCreated")
    void testCreateHospitalBranchAndReturnCreated() throws Exception {
        // Arrange
        when(branchService.createHospitalBranch(eq(1L), any(CreateHospitalBranchRequestDto.class)))
                .thenReturn(hospitalResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/branches/hospital/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hospitalRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Hospital Central"))
                .andExpect(jsonPath("$.bedCapacity").value(50));

        verify(branchService, times(1)).createHospitalBranch(eq(1L), any(CreateHospitalBranchRequestDto.class));
    }

    @Test
    @DisplayName("shouldGetBranchById")
    void testGetBranchById() throws Exception {
        // Arrange
        ClinicBranch branch = new ClinicBranch();
        branch.setId(1L);
        branch.setName("Sucursal Centro");
        branch.setAddress("Calle Principal 123");
        branch.setHasLaboratory(true);

        when(branchService.findById(1L)).thenReturn(branch);

        // Act & Assert
        mockMvc.perform(get("/api/v1/branches/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Sucursal Centro"));

        verify(branchService, times(1)).findById(1L);
    }

    @Test
    @DisplayName("shouldGetBranchesByCompanyId")
    void testGetBranchesByCompanyId() throws Exception {
        // Arrange
        when(branchService.findByCompanyId(1L)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/branches/company/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.isA(java.util.List.class)));

        verify(branchService, times(1)).findByCompanyId(1L);
    }

    @Test
    @DisplayName("shouldDeleteBranchAndReturnNoContent")
    void testDeleteBranchAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(branchService).deleteById(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/branches/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(branchService, times(1)).deleteById(1L);
    }
}

