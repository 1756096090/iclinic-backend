package com.iclinic.iclinicbackend.modules.admin.dto;

import com.iclinic.iclinicbackend.shared.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSuperAdminRequestDto {

    @NotBlank(message = "El nombre es requerido")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    private String lastName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no es válido")
    private String email;

    private String phone;

    @NotNull(message = "El tipo de usuario es requerido")
    private UserType userType;

    private String documentNumber;
    private String nationality;
}

