package com.iclinic.iclinicbackend.modules.user.mapper;

import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.modules.user.entity.*;
import com.iclinic.iclinicbackend.shared.enums.UserType;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {
        UserType userType = resolveUserType(user);

        UserResponseDto.UserResponseDtoBuilder builder = UserResponseDto.builder()
                .id(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .roleDisplayName(user.getRole().getDisplayName())
                .userType(userType)
                .documentType(user.getDocumentType())
                .documentTypeDisplayName(user.getDocumentType() != null
                        ? user.getDocumentType().getDisplayName() : null)
                .documentNumber(user.getDocumentNumber())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getName() : null);

        if (user instanceof InternationalUser intlUser) {
            builder.nationality(intlUser.getNationality());
        }

        return builder.build();
    }

    private UserType resolveUserType(User user) {
        if (user instanceof EcuadorianUser)   return UserType.ECUADORIAN;
        if (user instanceof ColombianUser)    return UserType.COLOMBIAN;
        if (user instanceof PeruvianUser)     return UserType.PERUVIAN;
        if (user instanceof InternationalUser) return UserType.INTERNATIONAL;
        throw new IllegalArgumentException("Tipo de usuario no reconocido: " + user.getClass().getName());
    }
}
