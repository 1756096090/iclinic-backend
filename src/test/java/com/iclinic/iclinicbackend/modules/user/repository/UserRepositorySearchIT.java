package com.iclinic.iclinicbackend.modules.user.repository;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.user.entity.ColombianUser;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.SubjectType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@DisplayName("UserRepository Search Tests")
class UserRepositorySearchIT extends AbstractPostgresIT {
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CompanyRepository companyRepository;
    @Test
    void localAdministratorIsSeededWithPlatformAccess() {
        var user = userRepository.findByEmail("isaaccs2003@gmail.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getIsPlatformAdmin()).isTrue();
        // Un administrador de plataforma NO pertenece a ninguna clinica: su acceso
        // viene de is_platform_admin. Antes el seed le ponia company_id = 1, una
        // fila que se contradecia a si misma y que la guardia previa al DROP
        // detecto. Ver V10.
        assertThat(user.getCompany())
                .as("el admin de plataforma no cuelga de ninguna empresa")
                .isNull();
        assertThat(user.getBranch()).isNull();
        // La credencial vive en Keycloak: aqui solo hay proyeccion del sujeto.
        assertThat(user.getKeycloakUserId()).isNotNull();
        assertThat(user.getSubjectType()).isEqualTo(SubjectType.HUMAN);
    }

    @Test
    @DisplayName("shouldSearchUsersByBranchAndTextWithLimit")
    void shouldSearchUsersByBranchAndTextWithLimit() {
        EcuadorianCompany company = companyRepository.save(new EcuadorianCompany("Clinica Central Test", "1799999999999"));
        ClinicBranch branch1 = branchRepository.save(new ClinicBranch("Sucursal 1", "Av. Uno", true, company, "America/Guayaquil"));
        ClinicBranch branch2 = branchRepository.save(new ClinicBranch("Sucursal 2", "Av. Dos", true, company, "America/Guayaquil"));
        EcuadorianUser veronica = new EcuadorianUser();
        veronica.setFirstName("Veronica");
        veronica.setLastName("Salazar");
        veronica.setEmail("veronica@clinic.com");
        veronica.setPhone("+593900000001");
        veronica.setRole(UserRole.ADMIN);
        veronica.setDocumentType(DocumentType.CEDULA_EC);
        veronica.setDocumentNumber("1712345678");
        veronica.setCompany(company);
        veronica.setBranch(branch1);
        userRepository.save(veronica);
        ColombianUser veronicaOtherBranch = new ColombianUser();
        veronicaOtherBranch.setFirstName("Veronica");
        veronicaOtherBranch.setLastName("Torres");
        veronicaOtherBranch.setEmail("veronica.other@clinic.com");
        veronicaOtherBranch.setPhone("+573000000002");
        veronicaOtherBranch.setRole(UserRole.DENTIST);
        veronicaOtherBranch.setDocumentType(DocumentType.CEDULA_CO);
        veronicaOtherBranch.setDocumentNumber("12345678");
        veronicaOtherBranch.setCompany(company);
        veronicaOtherBranch.setBranch(branch2);
        userRepository.save(veronicaOtherBranch);
        var results = userRepository.searchByBranchIdAndText(
                branch1.getId(),
                "ver",
                PageRequest.of(0, 1, Sort.by("firstName").ascending())
        );
        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getFirstName()).isEqualTo("Veronica");
        assertThat(results.getContent().get(0).getBranch().getId()).isEqualTo(branch1.getId());
    }

    @Test
    @DisplayName("shouldFindOnlyActiveDoctorsByBranch")
    void shouldFindOnlyActiveDoctorsByBranch() {
        EcuadorianCompany company = companyRepository.save(new EcuadorianCompany("Clinica Central Test", "1799999999998"));
        ClinicBranch branch = branchRepository.save(new ClinicBranch("Sucursal 1", "Av. Uno", true, company, "America/Guayaquil"));

        EcuadorianUser activeDoctor = new EcuadorianUser();
        activeDoctor.setFirstName("Ana");
        activeDoctor.setLastName("Lopez");
        activeDoctor.setEmail("ana.lopez@clinic.com");
        activeDoctor.setPhone("+593900000010");
        activeDoctor.setRole(UserRole.DENTIST);
        activeDoctor.setDocumentType(DocumentType.CEDULA_EC);
        activeDoctor.setDocumentNumber("1712345679");
        activeDoctor.setCompany(company);
        activeDoctor.setBranch(branch);
        userRepository.save(activeDoctor);

        EcuadorianUser inactiveDoctor = new EcuadorianUser();
        inactiveDoctor.setFirstName("Bruno");
        inactiveDoctor.setLastName("Perez");
        inactiveDoctor.setEmail("bruno.perez@clinic.com");
        inactiveDoctor.setPhone("+593900000011");
        inactiveDoctor.setRole(UserRole.DENTIST);
        inactiveDoctor.setDocumentType(DocumentType.CEDULA_EC);
        inactiveDoctor.setDocumentNumber("1712345680");
        inactiveDoctor.setCompany(company);
        inactiveDoctor.setBranch(branch);
        inactiveDoctor.setActive(false);
        userRepository.save(inactiveDoctor);

        var doctors = userRepository.findByBranchIdAndRoleAndActiveTrueOrderByFirstNameAsc(branch.getId(), UserRole.DENTIST);

        assertThat(doctors).hasSize(1);
        assertThat(doctors.get(0).getFirstName()).isEqualTo("Ana");
    }
}
