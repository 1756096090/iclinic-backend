package com.iclinic.iclinicbackend.modules.crm.message.controller;

import com.iclinic.iclinicbackend.modules.crm.message.dto.SendMessageRequestDto;
import com.iclinic.iclinicbackend.modules.crm.message.service.MessageService;
import com.iclinic.iclinicbackend.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crm/messages")
@RequiredArgsConstructor
@Tag(name = "CRM Messages", description = "Envío y consulta de mensajes")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "Listar todos los mensajes")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<java.util.List<com.iclinic.iclinicbackend.modules.crm.message.dto.MessageResponseDto>> findAll() {
        return ResponseEntity.ok(messageService.findAll().stream()
                .map(com.iclinic.iclinicbackend.modules.crm.mapper.MessageMapper::toResponseDto)
                .toList());
    }

    @PostMapping("/send")
    @Operation(summary = "Enviar mensaje de texto", description = "Envía un mensaje de salida a través del canal configurado")
    @ApiResponse(responseCode = "200", description = "Mensaje enviado")
    @ApiResponse(responseCode = "400", description = "Error de validación",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<com.iclinic.iclinicbackend.modules.crm.message.dto.MessageResponseDto> send(@Valid @RequestBody SendMessageRequestDto dto) {
        return ResponseEntity.ok(com.iclinic.iclinicbackend.modules.crm.mapper.MessageMapper.toResponseDto(messageService.sendText(dto)));
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "Listar mensajes de una conversación", description = "Retorna los mensajes ordenados por fecha ASC")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<java.util.List<com.iclinic.iclinicbackend.modules.crm.message.dto.MessageResponseDto>> findByConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(messageService.findByConversation(conversationId).stream()
                .map(com.iclinic.iclinicbackend.modules.crm.mapper.MessageMapper::toResponseDto)
                .toList());
    }
}
