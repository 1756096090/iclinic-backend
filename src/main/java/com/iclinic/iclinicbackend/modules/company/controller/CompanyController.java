package com.iclinic.iclinicbackend.modules.company.controller;

import com.iclinic.iclinicbackend.modules.company.dto.ColombianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CompanyUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateColombianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateEcuadorianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.EcuadorianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.service.CompanyService;
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
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Manage companies by type")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @Operation(
        summary = "Crear empresa (unificado)",
        description = "Crea una empresa de cualquier tipo usando el campo companyType (ECUADORIAN | COLOMBIAN). " +
                      "ECUADORIAN requiere taxId = RUC (13 dígitos). COLOMBIAN requiere taxId = NIT (9-10 dígitos)."
    )
    @ApiResponse(responseCode = "201", description = "Empresa creada",
            content = @Content(schema = @Schema(implementation = CompanyUnifiedResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Error de validación",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Identificador fiscal duplicado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CompanyUnifiedResponseDto> createCompany(
            @Valid @RequestBody CreateCompanyUnifiedRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createCompany(dto));
    }

    @PostMapping("/ecuadorian")
    @Operation(summary = "Create ecuadorian company", description = "Creates a new ecuadorian company with RUC")
    @ApiResponse(responseCode = "201", description = "Company created",
            content = @Content(schema = @Schema(implementation = EcuadorianCompanyResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate RUC",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EcuadorianCompanyResponseDto> createEcuadorianCompany(
            @Valid @RequestBody CreateEcuadorianCompanyRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createEcuadorianCompany(dto));
    }

    @PostMapping("/colombian")
    @Operation(summary = "Create colombian company", description = "Creates a new colombian company with NIT")
    @ApiResponse(responseCode = "201", description = "Company created",
            content = @Content(schema = @Schema(implementation = ColombianCompanyResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate NIT",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ColombianCompanyResponseDto> createColombianCompany(
            @Valid @RequestBody CreateColombianCompanyRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createColombianCompany(dto));
    }

    @GetMapping
    @Operation(summary = "Get all companies", description = "Returns a list of all companies")
    @ApiResponse(responseCode = "200", description = "List returned successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID", description = "Returns a specific company by its ID")
    @ApiResponse(responseCode = "200", description = "Company found")
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete company", description = "Deletes a specific company by its ID")
    @ApiResponse(responseCode = "204", description = "Deleted successfully")
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

