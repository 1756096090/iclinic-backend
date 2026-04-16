package com.iclinic.iclinicbackend.modules.branch.service;

import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.entity.HospitalBranch;
import com.iclinic.iclinicbackend.modules.branch.mapper.BranchMapper;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BranchServiceImpl Tests")
class BranchServiceImplTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private BranchServiceImpl branchService;

    private Company company;
    private CreateClinicBranchRequestDto clinicBranchDto;
    private CreateHospitalBranchRequestDto hospitalBranchDto;
    private ClinicBranch clinicBranch;
    private HospitalBranch hospitalBranch;

    @BeforeEach
    void setUp() {
        // Setup Company
        company = new EcuadorianCompany();
        company.setId(1L);
        company.setName("Clinica Dental XYZ");
        company.setBranches(new ArrayList<>());

        // Setup Clinic Branch DTO
        clinicBranchDto = new CreateClinicBranchRequestDto();
        clinicBranchDto.setName("Sucursal Centro");
        clinicBranchDto.setAddress("Calle Principal 123");
        clinicBranchDto.setHasLaboratory(true);

        // Setup Hospital Branch DTO
        hospitalBranchDto = new CreateHospitalBranchRequestDto();
        hospitalBranchDto.setName("Hospital Central");
        hospitalBranchDto.setAddress("Av. Hospital 456");
        hospitalBranchDto.setBedCapacity(50);

        // Setup Clinic Branch
        clinicBranch = new ClinicBranch();
        clinicBranch.setId(1L);
        clinicBranch.setName("Sucursal Centro");
        clinicBranch.setAddress("Calle Principal 123");
        clinicBranch.setHasLaboratory(true);
        clinicBranch.setCompany(company);

        // Setup Hospital Branch
        hospitalBranch = new HospitalBranch();
        hospitalBranch.setId(2L);
        hospitalBranch.setName("Hospital Central");
        hospitalBranch.setAddress("Av. Hospital 456");
        hospitalBranch.setBedCapacity(50);
        hospitalBranch.setCompany(company);
    }

    @Test
    @DisplayName("shouldCreateClinicBranchSuccessfully")
    void testCreateClinicBranchSuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.save(any(ClinicBranch.class))).thenReturn(clinicBranch);
        when(branchMapper.toClinicResponseDto(clinicBranch)).thenReturn(null);

        // Act
        branchService.createClinicBranch(1L, clinicBranchDto);

        // Assert
        verify(companyRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).save(any(ClinicBranch.class));
        verify(branchMapper, times(1)).toClinicResponseDto(clinicBranch);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenCompanyNotFoundForClinicBranch")
    void testThrowExceptionWhenCompanyNotFoundForClinicBranch() {
        // Arrange
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompanyNotFoundException.class, () ->
            branchService.createClinicBranch(99L, clinicBranchDto)
        );
        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateHospitalBranchSuccessfully")
    void testCreateHospitalBranchSuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.save(any(HospitalBranch.class))).thenReturn(hospitalBranch);
        when(branchMapper.toHospitalResponseDto(hospitalBranch)).thenReturn(null);

        // Act
        branchService.createHospitalBranch(1L, hospitalBranchDto);

        // Assert
        verify(companyRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).save(any(HospitalBranch.class));
        verify(branchMapper, times(1)).toHospitalResponseDto(hospitalBranch);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenCompanyNotFoundForHospitalBranch")
    void testThrowExceptionWhenCompanyNotFoundForHospitalBranch() {
        // Arrange
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompanyNotFoundException.class, () ->
            branchService.createHospitalBranch(99L, hospitalBranchDto)
        );
        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldFindBranchByIdSuccessfully")
    void testFindBranchByIdSuccessfully() {
        // Arrange
        when(branchRepository.findById(1L)).thenReturn(Optional.of(clinicBranch));

        // Act
        var result = branchService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(clinicBranch.getId(), result.getId());
        verify(branchRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("shouldThrowNotFoundExceptionWhenBranchDoesNotExist")
    void testThrowNotFoundExceptionWhenBranchDoesNotExist() {
        // Arrange
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BranchNotFoundException.class, () ->
            branchService.findById(99L)
        );
        verify(branchRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("shouldFindBranchesByCompanyIdSuccessfully")
    void testFindBranchesByCompanyIdSuccessfully() {
        // Arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(clinicBranch);
        branches.add(hospitalBranch);
        when(branchRepository.findByCompanyId(1L)).thenReturn(branches);

        // Act
        var result = branchService.findByCompanyId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(branchRepository, times(1)).findByCompanyId(1L);
    }

    @Test
    @DisplayName("shouldDeleteBranchSuccessfully")
    void testDeleteBranchSuccessfully() {
        // Arrange
        when(branchRepository.findById(1L)).thenReturn(Optional.of(clinicBranch));
        doNothing().when(branchRepository).delete(clinicBranch);

        // Act
        branchService.deleteById(1L);

        // Assert
        verify(branchRepository, times(1)).findById(1L);
        verify(branchRepository, times(1)).delete(clinicBranch);
    }

    @Test
    @DisplayName("shouldThrowNotFoundExceptionWhenDeletingNonExistentBranch")
    void testThrowNotFoundExceptionWhenDeletingNonExistentBranch() {
        // Arrange
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BranchNotFoundException.class, () ->
            branchService.deleteById(99L)
        );
        verify(branchRepository, never()).delete(any());
    }
}

