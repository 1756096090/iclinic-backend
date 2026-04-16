package com.iclinic.iclinicbackend.modules.user.service;

import com.iclinic.iclinicbackend.modules.user.dto.CreateUserRequestDto;
import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.shared.enums.UserRole;

import java.util.List;

// Interfaz de servicio de usuarios
public interface UserService {
    UserResponseDto create(CreateUserRequestDto dto);
    UserResponseDto findById(Long id);
    List<UserResponseDto> findAll();
    List<UserResponseDto> findByRole(UserRole role);
    List<UserResponseDto> findByCompanyId(Long companyId);
    List<UserResponseDto> findByBranchId(Long branchId);
    UserResponseDto deactivate(Long id);
    void deleteById(Long id);
}

