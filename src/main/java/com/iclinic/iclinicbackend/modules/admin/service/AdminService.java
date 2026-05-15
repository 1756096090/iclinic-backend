package com.iclinic.iclinicbackend.modules.admin.service;

import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingRequestDto;
import com.iclinic.iclinicbackend.modules.admin.dto.ClientOnboardingResponseDto;
import com.iclinic.iclinicbackend.modules.admin.dto.InviteUserRequestDto;
import com.iclinic.iclinicbackend.modules.auth.dto.AuthUserResponseDto;
import com.iclinic.iclinicbackend.modules.auth.service.CurrentUserService;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.*;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.user.entity.*;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ClientOnboardingResponseDto createClient(ClientOnboardingRequestDto dto) {
        currentUserService.isSuperAdmin();
        if (!currentUserService.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo SUPER_ADMIN puede crear clientes");
        }

        Company company = buildCompany(dto.getCompany());
        company = companyRepository.save(company);

        ClinicBranch branch = new ClinicBranch(
                dto.getBranch().getName(),
                dto.getBranch().getAddress(),
                dto.getBranch().getHasLaboratory() != null && dto.getBranch().getHasLaboratory(),
                company
        );
        branch = (ClinicBranch) branchRepository.save(branch);

        User user = buildUser(dto.getAdminUser(), company.getId(), branch.getId());
        user.setCompany(company);
        user.setBranch(branch);
        user.setActive(true);
        user = userRepository.save(user);

        AuthUserResponseDto userDto = toAuthDto(user);

        return ClientOnboardingResponseDto.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .user(userDto)
                .message("El usuario ya puede ingresar con Google, Microsoft, Facebook o correo usando el email registrado.")
                .build();
    }

    public AuthUserResponseDto inviteUser(InviteUserRequestDto dto) {
        var currentUser = currentUserService.getCurrentUser();
        UserRole currentRole = currentUser.getRole();

        if (currentRole != UserRole.SUPER_ADMIN && currentRole != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso para invitar usuarios");
        }
        if (currentRole == UserRole.ADMIN) {
            if (dto.getRole() == UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN no puede crear SUPER_ADMIN");
            }
            currentUserService.assertCanAccessCompany(dto.getCompanyId());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }

        var company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        var branch = dto.getBranchId() != null
                ? branchRepository.findById(dto.getBranchId()).orElse(null)
                : null;

        User user = buildUserFromInvite(dto);
        user.setCompany(company);
        user.setBranch(branch);
        user.setActive(true);
        user = userRepository.save(user);

        return toAuthDto(user);
    }

    private Company buildCompany(ClientOnboardingRequestDto.CompanyData data) {
        return switch (data.getCompanyType()) {
            case ECUADORIAN -> {
                EcuadorianCompany c = new EcuadorianCompany();
                c.setName(data.getName());
                c.setRuc(data.getTaxId());
                c.setCompanyType(CompanyType.ECUADORIAN);
                yield c;
            }
            case COLOMBIAN -> {
                ColombianCompany c = new ColombianCompany();
                c.setName(data.getName());
                c.setNit(data.getTaxId());
                c.setCompanyType(CompanyType.COLOMBIAN);
                yield c;
            }
            default -> {
                EcuadorianCompany c = new EcuadorianCompany();
                c.setName(data.getName());
                c.setRuc(data.getTaxId());
                c.setCompanyType(data.getCompanyType());
                yield c;
            }
        };
    }

    private User buildUser(ClientOnboardingRequestDto.AdminUserData data, Long companyId, Long branchId) {
        User user = createUserByType(data.getUserType(), data.getDocumentNumber(), data.getNationality());
        user.setFirstName(data.getFirstName());
        user.setLastName(data.getLastName());
        user.setEmail(data.getEmail());
        user.setPhone(data.getPhone());
        user.setRole(data.getRole());
        if (data.getDocumentType() != null) {
            user.setDocumentType(data.getDocumentType());
        }
        return user;
    }

    private User buildUserFromInvite(InviteUserRequestDto dto) {
        User user = createUserByType(dto.getUserType(), dto.getDocumentNumber(), dto.getNationality());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        if (dto.getDocumentType() != null) {
            user.setDocumentType(dto.getDocumentType());
        }
        return user;
    }

    private User createUserByType(UserType type, String documentNumber, String nationality) {
        return switch (type) {
            case ECUADORIAN -> { EcuadorianUser u = new EcuadorianUser(); u.setDocumentNumber(documentNumber); yield u; }
            case COLOMBIAN  -> { ColombianUser  u = new ColombianUser();  u.setDocumentNumber(documentNumber); yield u; }
            case PERUVIAN   -> { PeruvianUser   u = new PeruvianUser();   u.setDocumentNumber(documentNumber); yield u; }
            case INTERNATIONAL -> {
                InternationalUser u = new InternationalUser();
                u.setPassportNumber(documentNumber);
                u.setNationality(nationality);
                yield u;
            }
        };
    }

    private AuthUserResponseDto toAuthDto(User user) {
        return AuthUserResponseDto.builder()
                .id(user.getId())
                .externalAuthId(user.getExternalAuthId())
                .authProvider(user.getAuthProvider())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .role(user.getRole())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .active(user.getActive())
                .build();
    }
}
