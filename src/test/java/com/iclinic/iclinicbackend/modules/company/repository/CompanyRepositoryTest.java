package com.iclinic.iclinicbackend.modules.company.repository;

import com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@DisplayName("CompanyRepository Tests")
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EcuadorianCompanyRepository ecuadorianCompanyRepository;

    @Autowired
    private ColombianCompanyRepository colombianCompanyRepository;

    private EcuadorianCompany ecuadorianCompany;
    private ColombianCompany colombianCompany;

    @BeforeEach
    void setUp() {
        ecuadorianCompany = new EcuadorianCompany("Clinica Dental XYZ", "1712345678901");
        colombianCompany  = new ColombianCompany("Clinica ABC", "901234567");
    }

    @Test
    @DisplayName("shouldSaveEcuadorianCompanySuccessfully")
    void testSaveEcuadorianCompanySuccessfully() {
        // Act
        EcuadorianCompany saved = ecuadorianCompanyRepository.save(ecuadorianCompany);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("1712345678901", saved.getRuc());
        assertEquals("Clinica Dental XYZ", saved.getName());
    }

    @Test
    @DisplayName("shouldSaveColombianCompanySuccessfully")
    void testSaveColombianCompanySuccessfully() {
        // Act
        ColombianCompany saved = colombianCompanyRepository.save(colombianCompany);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("901234567", saved.getNit());
        assertEquals("Clinica ABC", saved.getName());
    }

    @Test
    @DisplayName("shouldFindEcuadorianCompanyByRuc")
    void testFindEcuadorianCompanyByRuc() {
        // Arrange
        ecuadorianCompanyRepository.save(ecuadorianCompany);

        // Act
        Optional<EcuadorianCompany> found = ecuadorianCompanyRepository.findByRuc("1712345678901");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Clinica Dental XYZ", found.get().getName());
    }

    @Test
    @DisplayName("shouldFindColombianCompanyByNit")
    void testFindColombianCompanyByNit() {
        // Arrange
        colombianCompanyRepository.save(colombianCompany);

        // Act
        Optional<ColombianCompany> found = colombianCompanyRepository.findByNit("901234567");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Clinica ABC", found.get().getName());
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenRucNotFound")
    void testReturnEmptyWhenRucNotFound() {
        // Act
        Optional<EcuadorianCompany> found = ecuadorianCompanyRepository.findByRuc("9999999999999");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenNitNotFound")
    void testReturnEmptyWhenNitNotFound() {
        // Act
        Optional<ColombianCompany> found = colombianCompanyRepository.findByNit("999999999");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("shouldRetrieveCompanyByIdUsing ParentRepository")
    void testRetrieveCompanyByIdUsingParentRepository() {
        // Arrange
        EcuadorianCompany saved = ecuadorianCompanyRepository.save(ecuadorianCompany);

        // Act
        Optional<com.iclinic.iclinicbackend.modules.company.entity.Company> found =
            companyRepository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertInstanceOf(EcuadorianCompany.class, found.get());
    }

    @Test
    @DisplayName("shouldDeleteCompanySuccessfully")
    void testDeleteCompanySuccessfully() {
        // Arrange
        EcuadorianCompany saved = ecuadorianCompanyRepository.save(ecuadorianCompany);
        Long id = saved.getId();

        // Act
        companyRepository.deleteById(id);

        // Assert
        assertFalse(companyRepository.findById(id).isPresent());
    }
}

