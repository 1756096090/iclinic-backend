package com.iclinic.iclinicbackend.modules.user.dto;

import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO unificado para crear usuarios de cualquier tipo.
 *
 * Según userType los campos requeridos son:
 *   ECUADORIAN  → documentType (CEDULA_EC | RUC_EC)  + documentNumber
 *   COLOMBIAN   → documentType (CEDULA_CO | NIT_CO)  + documentNumber
 *   PERUVIAN    → documentType (DNI_PE    | RUC_PE)  + documentNumber
 *   INTERNATIONAL → documentNumber (pasaporte) + nationality
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateUserRequest", description = "DTO unificado para crear usuarios del sistema")
public class CreateUserRequestDto {

    @NotBlank(message = "El nombre es requerido")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String lastName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no es válido")
    @Schema(description = "Email del usuario", example = "juan.perez@iclinic.com")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Contraseña", example = "secret123")
    private String password;

    @Schema(description = "Teléfono del usuario", example = "+593987654321")
    private String phone;

    @NotNull(message = "El rol es requerido")
    @Schema(
        description = "Rol del usuario",
        allowableValues = {"ADMIN", "DENTIST", "ASSISTANT", "RECEPTIONIST", "PATIENT"},
        example = "DENTIST"
    )
    private UserRole role;

    @NotNull(message = "El tipo de usuario es requerido")
    @Schema(
        description = "Tipo de usuario según país",
        allowableValues = {"ECUADORIAN", "COLOMBIAN", "PERUVIAN", "INTERNATIONAL"},
        example = "ECUADORIAN"
    )
    private UserType userType;

    // --- Campos específicos por subtipo ---

    @Schema(
        description = "Tipo de documento (requerido para ECUADORIAN, COLOMBIAN, PERUVIAN). " +
                      "ECUADORIAN: CEDULA_EC | RUC_EC. COLOMBIAN: CEDULA_CO | NIT_CO. PERUVIAN: DNI_PE | RUC_PE.",
        example = "CEDULA_EC"
    )
    private DocumentType documentType;

    @Schema(description = "Número de documento (cédula, DNI, RUC, pasaporte)", example = "1712345678")
    private String documentNumber;

    @Schema(description = "Nacionalidad (solo para INTERNATIONAL)", example = "Venezolana")
    private String nationality;

    // --- Relaciones opcionales ---

    @Schema(description = "ID de la empresa a la que pertenece (opcional)", example = "1")
    private Long companyId;

    @Schema(description = "ID de la sucursal a la que pertenece (opcional)", example = "1")
    private Long branchId;
}
