package com.iclinic.iclinicbackend.modules.admin.dto;

import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteUserRequestDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotNull
    private UserRole role;

    @NotNull
    private Long companyId;

    private Long branchId;

    @NotNull
    private UserType userType;

    private DocumentType documentType;
    private String documentNumber;
    private String nationality;
}
