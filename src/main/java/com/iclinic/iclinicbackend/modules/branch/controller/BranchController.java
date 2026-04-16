package com.iclinic.iclinicbackend.modules.branch.controller;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.ClinicBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateBranchUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.HospitalBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.service.BranchService;
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
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Manage branches by type")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Listar todas las sucursales", description = "Retorna todas las sucursales del sistema")
    @ApiResponse(responseCode = "200", description = "Lista retornada correctamente")
    public ResponseEntity<List<BranchUnifiedResponseDto>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @PostMapping
    @Operation(
        summary = "Crear sucursal (unificado)",
        description = "Crea una sucursal de cualquier tipo. CLINIC requiere hasLaboratory. HOSPITAL requiere bedCapacity."
    )
    @ApiResponse(responseCode = "201", description = "Sucursal creada",
            content = @Content(schema = @Schema(implementation = BranchUnifiedResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Error de validación o campo faltante",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BranchUnifiedResponseDto> createBranch(
            @Valid @RequestBody CreateBranchUnifiedRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.createBranch(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sucursal", description = "Actualiza nombre, dirección y campos específicos de una sucursal")
    @ApiResponse(responseCode = "200", description = "Sucursal actualizada",
            content = @Content(schema = @Schema(implementation = BranchUnifiedResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "No encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BranchUnifiedResponseDto> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchUnifiedRequestDto dto) {
        return ResponseEntity.ok(branchService.updateBranch(id, dto));
    }

    @PostMapping("/clinic/{companyId}")
    @Operation(summary = "Create clinic branch", description = "Creates a new clinic branch for a company")
    @ApiResponse(responseCode = "201", description = "Branch created",
            content = @Content(schema = @Schema(implementation = ClinicBranchResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Company not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ClinicBranchResponseDto> createClinicBranch(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateClinicBranchRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.createClinicBranch(companyId, dto));
    }

    @PostMapping("/hospital/{companyId}")
    @Operation(summary = "Create hospital branch", description = "Creates a new hospital branch for a company")
    @ApiResponse(responseCode = "201", description = "Branch created",
            content = @Content(schema = @Schema(implementation = HospitalBranchResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Company not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<HospitalBranchResponseDto> createHospitalBranch(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateHospitalBranchRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.createHospitalBranch(companyId, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by ID", description = "Returns a specific branch by its ID")
    @ApiResponse(responseCode = "200", description = "Branch found")
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Branch> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.findById(id));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get branches by company", description = "Returns all branches for a specific company")
    @ApiResponse(responseCode = "200", description = "List returned successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<Branch>> getBranchesByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(branchService.findByCompanyId(companyId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete branch", description = "Deletes a specific branch by its ID")
    @ApiResponse(responseCode = "204", description = "Deleted successfully")
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

