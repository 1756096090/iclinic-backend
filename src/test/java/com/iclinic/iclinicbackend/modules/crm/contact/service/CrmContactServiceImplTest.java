package com.iclinic.iclinicbackend.modules.crm.contact.service;

import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactRepository;
import com.iclinic.iclinicbackend.shared.enums.ChannelType;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrmContactService Tests")
@SuppressWarnings("deprecation")
class CrmContactServiceImplTest {

    @Mock private CrmContactRepository contactRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private BranchRepository branchRepository;

    @InjectMocks
    private CrmContactServiceImpl contactService;

    private EcuadorianCompany company;
    private CrmContact existingContact;

    @BeforeEach
    void setUp() {
        company = new EcuadorianCompany("Clinica XYZ", "1712345678901");
        company.setId(1L);

        existingContact = CrmContact.builder()
                .id(10L)
                .company(company)
                .fullName("Juan Pérez")
                .phone("+593987654321")
                .sourceChannel(ChannelType.WHATSAPP)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("shouldReturnExistingContactWhenPhoneFound")
    void testReturnExistingContact() {
        when(contactRepository.findByCompanyIdAndPhone(1L, "+593987654321"))
                .thenReturn(Optional.of(existingContact));

        CrmContact result = contactService.findOrCreate(1L, null, "Juan Pérez", "+593987654321");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("+593987654321", result.getPhone());
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateNewContactWhenPhoneNotFound")
    void testCreateNewContact() {
        when(contactRepository.findByCompanyIdAndPhone(1L, "+593111222333"))
                .thenReturn(Optional.empty());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(CrmContact.class))).thenAnswer(inv -> {
            CrmContact c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        CrmContact result = contactService.findOrCreate(1L, null, "Nuevo Cliente", "+593111222333");

        assertNotNull(result);
        assertEquals("Nuevo Cliente", result.getFullName());
        assertEquals("+593111222333", result.getPhone());
        assertEquals(ChannelType.WHATSAPP, result.getSourceChannel());
        verify(contactRepository).save(any(CrmContact.class));
    }

    @Test
    @DisplayName("shouldUsePhoneAsNameWhenFullNameIsBlank")
    void testUsePhoneAsNameWhenBlank() {
        when(contactRepository.findByCompanyIdAndPhone(1L, "+593000000000"))
                .thenReturn(Optional.empty());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(CrmContact.class))).thenAnswer(inv -> inv.getArgument(0));

        CrmContact result = contactService.findOrCreate(1L, null, "   ", "+593000000000");

        assertEquals("+593000000000", result.getFullName());
    }

    @Test
    @DisplayName("shouldAssignBranchWhenBranchIdProvided")
    void testAssignBranchWhenProvided() {
        ClinicBranch branch = new ClinicBranch();
        branch.setId(5L);
        branch.setCompany(company);

        when(contactRepository.findByCompanyIdAndPhone(1L, "+593999000000"))
                .thenReturn(Optional.empty());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(5L)).thenReturn(Optional.of(branch));
        when(contactRepository.save(any(CrmContact.class))).thenAnswer(inv -> inv.getArgument(0));

        CrmContact result = contactService.findOrCreate(1L, 5L, "Test", "+593999000000");

        assertNotNull(result.getBranch());
        assertEquals(5L, result.getBranch().getId());
    }

    @Test
    @DisplayName("shouldThrowWhenCompanyNotFound")
    void testThrowWhenCompanyNotFound() {
        when(contactRepository.findByCompanyIdAndPhone(99L, "+593000000000"))
                .thenReturn(Optional.empty());
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () ->
                contactService.findOrCreate(99L, null, "Test", "+593000000000"));
    }
}
