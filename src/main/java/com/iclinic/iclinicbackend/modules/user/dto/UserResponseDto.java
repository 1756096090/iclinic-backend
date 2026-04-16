package com.iclinic.iclinicbackend.modules.user.dto;

import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para usuario.
 * No expone la contraseña.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UserResponse", description = "DTO de respuesta para usuario")
public class UserResponseDto {

    @Schema(description = "ID del usuario")
    private Long id;

    @Schema(description = "Nombre completo")
    private String fullName;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Teléfono")
    private String phone;

    @Schema(description = "Rol del usuario")
    private UserRole role;

    @Schema(description = "Nombre del rol en español")
    private String roleDisplayName;

    @Schema(description = "Tipo de usuario (país)")
    private UserType userType;

    @Schema(description = "Tipo de documento")
    private DocumentType documentType;

    @Schema(description = "Nombre del tipo de documento")
    private String documentTypeDisplayName;

    @Schema(description = "Número de documento")
    private String documentNumber;

    @Schema(description = "Nacionalidad (solo para INTERNATIONAL)")
    private String nationality;

    @Schema(description = "Estado activo")
    private Boolean active;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "ID de la empresa (si aplica)")
    private Long companyId;

    @Schema(description = "Nombre de la empresa (si aplica)")
    private String companyName;

    @Schema(description = "ID de la sucursal (si aplica)")
    private Long branchId;

    @Schema(description = "Nombre de la sucursal (si aplica)")
    private String branchName;
}
