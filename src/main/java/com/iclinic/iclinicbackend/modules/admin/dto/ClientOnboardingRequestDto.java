package com.iclinic.iclinicbackend.modules.admin.dto;

import com.iclinic.iclinicbackend.shared.enums.CompanyType;
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
public class ClientOnboardingRequestDto {

    @NotNull
    private CompanyData company;

    @NotNull
    private BranchData branch;

    @NotNull
    private AdminUserData adminUser;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyData {
        @NotBlank
        private String name;
        private String taxId;
        @NotNull
        private CompanyType companyType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchData {
        @NotBlank
        private String name;
        @NotBlank
        private String address;
        private Boolean hasLaboratory;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserData {
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
        private UserType userType;
        private DocumentType documentType;
        private String documentNumber;
        private String nationality;
    }
}
