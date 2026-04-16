package com.iclinic.iclinicbackend.modules.user.service;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.user.dto.CreateUserRequestDto;
import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.modules.user.entity.*;
import com.iclinic.iclinicbackend.modules.user.mapper.UserMapper;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto create(CreateUserRequestDto dto) {
        log.info("Creating {} user with email: {}", dto.getUserType(), dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        User user = buildUserByType(dto);
        applyCommonFields(user, dto);
        assignRelations(user, dto);

        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id) {
        return userMapper.toResponseDto(loadUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findByCompanyId(Long companyId) {
        return userRepository.findByCompanyId(companyId).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findByBranchId(Long branchId) {
        return userRepository.findByBranchId(branchId).stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto deactivate(Long id) {
        User user = loadUser(id);
        user.setActive(false);
        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    public void deleteById(Long id) {
        userRepository.delete(loadUser(id));
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    private User buildUserByType(CreateUserRequestDto dto) {
        return switch (dto.getUserType()) {
            case ECUADORIAN, COLOMBIAN, PERUVIAN -> {
                if (dto.getDocumentType() == null || dto.getDocumentNumber() == null) {
                    throw new IllegalArgumentException(
                            "documentType y documentNumber son requeridos para usuarios " + dto.getUserType());
                }
                yield buildNationalUser(dto.getUserType(), dto.getDocumentNumber());
            }
            case INTERNATIONAL -> {
                if (dto.getDocumentNumber() == null) {
                    throw new IllegalArgumentException("documentNumber (pasaporte) es requerido para usuarios INTERNATIONAL");
                }
                InternationalUser u = new InternationalUser();
                u.setPassportNumber(dto.getDocumentNumber());
                u.setNationality(dto.getNationality());
                yield u;
            }
        };
    }

    private User buildNationalUser(com.iclinic.iclinicbackend.shared.enums.UserType type, String documentNumber) {
        return switch (type) {
            case ECUADORIAN -> { EcuadorianUser u = new EcuadorianUser(); u.setDocumentNumber(documentNumber); yield u; }
            case COLOMBIAN  -> { ColombianUser  u = new ColombianUser();  u.setDocumentNumber(documentNumber); yield u; }
            case PERUVIAN   -> { PeruvianUser   u = new PeruvianUser();   u.setDocumentNumber(documentNumber); yield u; }
            default -> throw new IllegalArgumentException("Tipo no soportado: " + type);
        };
    }

    private void applyCommonFields(User user, CreateUserRequestDto dto) {
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setDocumentType(
                dto.getUserType().name().equals("INTERNATIONAL") ? DocumentType.PASSPORT : dto.getDocumentType());
        user.setActive(true);
    }

    private void assignRelations(User user, CreateUserRequestDto dto) {
        if (dto.getCompanyId() != null) {
            Company company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new CompanyNotFoundException(dto.getCompanyId()));
            user.setCompany(company);
        }
        if (dto.getBranchId() != null) {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new BranchNotFoundException(dto.getBranchId()));
            user.setBranch(branch);
        }
    }
}


