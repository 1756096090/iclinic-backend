package com.iclinic.iclinicbackend.modules.crm.conversation.controller;

import com.iclinic.iclinicbackend.modules.crm.conversation.service.ConversationService;
import com.iclinic.iclinicbackend.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crm/conversations")
@RequiredArgsConstructor
@Tag(name = "CRM Conversations", description = "Gestión de conversaciones con pacientes/contactos")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @Operation(summary = "Listar todas las conversaciones")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto>> findAll() {
        return ResponseEntity.ok(conversationService.findAll().stream()
                .map(com.iclinic.iclinicbackend.modules.crm.mapper.ConversationMapper::toResponseDto)
                .toList());
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar conversaciones por empresa")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto>> findByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(conversationService.findByCompany(companyId).stream()
                .map(com.iclinic.iclinicbackend.modules.crm.mapper.ConversationMapper::toResponseDto)
                .toList());
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Listar conversaciones por sucursal")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto>> findByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(conversationService.findByBranch(branchId).stream()
                .map(com.iclinic.iclinicbackend.modules.crm.mapper.ConversationMapper::toResponseDto)
                .toList());
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Cerrar conversación")
    @ApiResponse(responseCode = "200", description = "Conversación cerrada")
    @ApiResponse(responseCode = "404", description = "Conversación no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto> close(@PathVariable Long id) {
        return ResponseEntity.ok(com.iclinic.iclinicbackend.modules.crm.mapper.ConversationMapper.toResponseDto(
                conversationService.close(id)));
    }

    @PatchMapping("/{conversationId}/assign/{userId}")
    @Operation(summary = "Asignar conversación a un usuario", description = "Asigna la conversación a un dentista o asistente")
    @ApiResponse(responseCode = "200", description = "Conversación asignada")
    @ApiResponse(responseCode = "400", description = "Conversación o usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<com.iclinic.iclinicbackend.modules.crm.conversation.dto.ConversationResponseDto> assign(
            @PathVariable Long conversationId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(com.iclinic.iclinicbackend.modules.crm.mapper.ConversationMapper.toResponseDto(
                conversationService.assign(conversationId, userId)));
    }
}
