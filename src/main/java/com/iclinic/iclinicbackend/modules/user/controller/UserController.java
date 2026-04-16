package com.iclinic.iclinicbackend.modules.user.controller;

import com.iclinic.iclinicbackend.modules.user.dto.CreateUserRequestDto;
import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.modules.user.service.UserService;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios del sistema")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario con un rol específico")
    @ApiResponse(responseCode = "201", description = "Usuario creado",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Error de validación",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email duplicado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Obtener usuarios por rol", description = "Filtra usuarios por su rol")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.findByRole(role));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Obtener usuarios por empresa")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<UserResponseDto>> getUsersByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(userService.findByCompanyId(companyId));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Obtener usuarios por sucursal")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<UserResponseDto>> getUsersByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(userService.findByBranchId(branchId));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar usuario", description = "Marca al usuario como inactivo sin eliminarlo")
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponseDto> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario")
    @ApiResponse(responseCode = "204", description = "Eliminado correctamente")
    @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

