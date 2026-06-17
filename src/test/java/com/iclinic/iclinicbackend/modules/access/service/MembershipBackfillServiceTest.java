package com.iclinic.iclinicbackend.modules.access.service;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipBackfillService - Fase B")
class MembershipBackfillServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyMembershipRepository membershipRepository;

    @InjectMocks private MembershipBackfillService service;

    private EcuadorianCompany company(Long id) {
        EcuadorianCompany c = new EcuadorianCompany();
        c.setId(id);
        c.setName("Clinica " + id);
        c.setRuc("17999999990" + id);
        c.setCompanyType(CompanyType.ECUADORIAN);
        return c;
    }

    private EcuadorianUser user(Long id, UserRole role, EcuadorianCompany company, ClinicBranch branch) {
        EcuadorianUser u = new EcuadorianUser();
        u.setId(id);
        u.setRole(role);
        u.setActive(true);
        u.setCompany(company);
        u.setBranch(branch);
        return u;
    }

    @Test
    @DisplayName("Crea membership para usuario con empresa, ADMIN como OWNER y copia sucursal")
    void createsMembershipForCompanyUser() {
        EcuadorianCompany c1 = company(1L);
        ClinicBranch branch = new ClinicBranch("Centro", "Av X", false, c1);
        branch.setId(10L);
        EcuadorianUser admin = user(1L, UserRole.ADMIN, c1, branch);

        when(userRepository.findAll()).thenReturn(List.of(admin));
        when(membershipRepository.existsByUserIdAndCompanyId(1L, 1L)).thenReturn(false);

        MembershipBackfillService.BackfillResult result = service.backfill();

        ArgumentCaptor<CompanyMembership> captor = ArgumentCaptor.forClass(CompanyMembership.class);
        verify(membershipRepository).save(captor.capture());
        CompanyMembership m = captor.getValue();

        assertEquals(UserRole.ADMIN, m.getRole());
        assertTrue(m.getIsOwner(), "ADMIN legacy debe migrar como OWNER");
        assertEquals(1, m.getBranches().size());
        assertTrue(m.getBranches().contains(branch));
        assertEquals(1, result.membershipsCreated());
        assertEquals(0, result.platformAdminsMarked());
    }

    @Test
    @DisplayName("SUPER_ADMIN se marca como platform admin y no genera membership")
    void marksSuperAdminAsPlatformAdmin() {
        EcuadorianUser superAdmin = user(99L, UserRole.SUPER_ADMIN, null, null);

        when(userRepository.findAll()).thenReturn(List.of(superAdmin));

        MembershipBackfillService.BackfillResult result = service.backfill();

        assertTrue(superAdmin.getIsPlatformAdmin());
        verify(userRepository).save(superAdmin);
        verify(membershipRepository, never()).save(any());
        assertEquals(1, result.platformAdminsMarked());
        assertEquals(0, result.membershipsCreated());
    }

    @Test
    @DisplayName("No duplica membership existente (idempotente)")
    void skipsExistingMembership() {
        EcuadorianCompany c1 = company(1L);
        EcuadorianUser dentist = user(2L, UserRole.DENTIST, c1, null);

        when(userRepository.findAll()).thenReturn(List.of(dentist));
        when(membershipRepository.existsByUserIdAndCompanyId(2L, 1L)).thenReturn(true);

        MembershipBackfillService.BackfillResult result = service.backfill();

        verify(membershipRepository, never()).save(any());
        assertEquals(0, result.membershipsCreated());
        assertEquals(1, result.membershipsSkipped());
    }

    @Test
    @DisplayName("Usuario sin empresa (paciente temporal) no genera membership")
    void skipsUserWithoutCompany() {
        EcuadorianUser orphan = user(3L, UserRole.PATIENT, null, null);

        when(userRepository.findAll()).thenReturn(List.of(orphan));

        MembershipBackfillService.BackfillResult result = service.backfill();

        verify(membershipRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        assertEquals(0, result.membershipsCreated());
    }
}
