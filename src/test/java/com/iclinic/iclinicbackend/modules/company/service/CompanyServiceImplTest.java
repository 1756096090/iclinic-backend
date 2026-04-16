package com.iclinic.iclinicbackend.modules.company.service;

import com.iclinic.iclinicbackend.modules.company.dto.CreateColombianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateEcuadorianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.mapper.CompanyMapper;
import com.iclinic.iclinicbackend.modules.company.repository.ColombianCompanyRepository;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.company.repository.EcuadorianCompanyRepository;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.DuplicateCompanyException;
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
@DisplayName("CompanyServiceImpl Tests")
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private EcuadorianCompanyRepository ecuadorianCompanyRepository;

    @Mock
    private ColombianCompanyRepository colombianCompanyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private CreateEcuadorianCompanyRequestDto ecuadorianRequestDto;
    private CreateColombianCompanyRequestDto colombianRequestDto;
    private EcuadorianCompany ecuadorianCompany;
    private ColombianCompany colombianCompany;

    @BeforeEach
    void setUp() {
        // Setup Ecuadorian DTO
        ecuadorianRequestDto = new CreateEcuadorianCompanyRequestDto();
        ecuadorianRequestDto.setName("Clinica Dental XYZ");
        ecuadorianRequestDto.setRuc("1712345678901");
        ecuadorianRequestDto.setBranches(new ArrayList<>());

        // Setup Colombian DTO
        colombianRequestDto = new CreateColombianCompanyRequestDto();
        colombianRequestDto.setName("Clinica ABC");
        colombianRequestDto.setNit("901234567");
        colombianRequestDto.setBranches(new ArrayList<>());

        // Setup Ecuadorian Company
        ecuadorianCompany = new EcuadorianCompany();
        ecuadorianCompany.setId(1L);
        ecuadorianCompany.setName("Clinica Dental XYZ");
        ecuadorianCompany.setRuc("1712345678901");
        ecuadorianCompany.setBranches(new ArrayList<>());

        // Setup Colombian Company
        colombianCompany = new ColombianCompany();
        colombianCompany.setId(2L);
        colombianCompany.setName("Clinica ABC");
        colombianCompany.setNit("901234567");
        colombianCompany.setBranches(new ArrayList<>());
    }

    @Test
    @DisplayName("shouldCreateEcuadorianCompanySuccessfully")
    void testCreateEcuadorianCompanySuccessfully() {
        // Arrange
        when(ecuadorianCompanyRepository.findByRuc(ecuadorianRequestDto.getRuc())).thenReturn(Optional.empty());
        when(companyRepository.save(any(EcuadorianCompany.class))).thenReturn(ecuadorianCompany);
        when(companyMapper.toEcuadorianResponseDto(ecuadorianCompany)).thenReturn(null);

        // Act
        companyService.createEcuadorianCompany(ecuadorianRequestDto);

        // Assert
        verify(ecuadorianCompanyRepository, times(1)).findByRuc(ecuadorianRequestDto.getRuc());
        verify(companyRepository, times(1)).save(any(EcuadorianCompany.class));
        verify(companyMapper, times(1)).toEcuadorianResponseDto(ecuadorianCompany);
    }

    @Test
    @DisplayName("shouldThrowDuplicateExceptionWhenRucExists")
    void testThrowDuplicateExceptionWhenRucExists() {
        // Arrange
        when(ecuadorianCompanyRepository.findByRuc(ecuadorianRequestDto.getRuc())).thenReturn(Optional.of(ecuadorianCompany));

        // Act & Assert
        assertThrows(DuplicateCompanyException.class, () ->
            companyService.createEcuadorianCompany(ecuadorianRequestDto)
        );
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateColombianCompanySuccessfully")
    void testCreateColombianCompanySuccessfully() {
        // Arrange
        when(colombianCompanyRepository.findByNit(colombianRequestDto.getNit())).thenReturn(Optional.empty());
        when(companyRepository.save(any(ColombianCompany.class))).thenReturn(colombianCompany);
        when(companyMapper.toColombianResponseDto(colombianCompany)).thenReturn(null);

        // Act
        companyService.createColombianCompany(colombianRequestDto);

        // Assert
        verify(colombianCompanyRepository, times(1)).findByNit(colombianRequestDto.getNit());
        verify(companyRepository, times(1)).save(any(ColombianCompany.class));
        verify(companyMapper, times(1)).toColombianResponseDto(colombianCompany);
    }

    @Test
    @DisplayName("shouldThrowDuplicateExceptionWhenNitExists")
    void testThrowDuplicateExceptionWhenNitExists() {
        // Arrange
        when(colombianCompanyRepository.findByNit(colombianRequestDto.getNit())).thenReturn(Optional.of(colombianCompany));

        // Act & Assert
        assertThrows(DuplicateCompanyException.class, () ->
            companyService.createColombianCompany(colombianRequestDto)
        );
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldFindCompanyByIdSuccessfully")
    void testFindCompanyByIdSuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(ecuadorianCompany));

        // Act
        var result = companyService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(ecuadorianCompany.getId(), result.getId());
        verify(companyRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("shouldThrowNotFoundExceptionWhenCompanyDoesNotExist")
    void testThrowNotFoundExceptionWhenCompanyDoesNotExist() {
        // Arrange
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompanyNotFoundException.class, () ->
            companyService.findById(99L)
        );
        verify(companyRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("shouldFindAllCompanies")
    void testFindAllCompanies() {
        // Arrange
        List<com.iclinic.iclinicbackend.modules.company.entity.Company> companies = new ArrayList<>();
        companies.add(ecuadorianCompany);
        companies.add(colombianCompany);
        when(companyRepository.findAll()).thenReturn(companies);

        // Act
        var result = companyService.findAll();

        // Assert
        assertEquals(2, result.size());
        verify(companyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("shouldDeleteCompanySuccessfully")
    void testDeleteCompanySuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(ecuadorianCompany));
        doNothing().when(companyRepository).delete(ecuadorianCompany);

        // Act
        companyService.deleteById(1L);

        // Assert
        verify(companyRepository, times(1)).findById(1L);
        verify(companyRepository, times(1)).delete(ecuadorianCompany);
    }

    @Test
    @DisplayName("shouldThrowNotFoundExceptionWhenDeletingNonExistentCompany")
    void testThrowNotFoundExceptionWhenDeletingNonExistentCompany() {
        // Arrange
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompanyNotFoundException.class, () ->
            companyService.deleteById(99L)
        );
        verify(companyRepository, never()).delete(any());
    }
}

