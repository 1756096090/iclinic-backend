package com.iclinic.iclinicbackend.modules.crm.channel.controller;

import com.iclinic.iclinicbackend.modules.crm.channel.dto.ChannelConnectionResponseDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.CreateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.SetWebhookRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.UpdateChannelConnectionRequestDto;
import com.iclinic.iclinicbackend.modules.crm.channel.dto.WebhookInfoDto;
import com.iclinic.iclinicbackend.modules.crm.channel.service.ChannelConnectionService;
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
@RequestMapping("/api/v1/crm/channels")
@RequiredArgsConstructor
@Tag(name = "CRM Channels", description = "Gestión de conexiones a canales de mensajería")
public class ChannelConnectionController {

    private final ChannelConnectionService channelConnectionService;

    @GetMapping
    @Operation(summary = "Listar todos los canales")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<ChannelConnectionResponseDto>> getAll() {
        return ResponseEntity.ok(channelConnectionService.getAll());
    }

    @PostMapping
    @Operation(summary = "Registrar canal", description = "Crea una nueva conexión a un canal (WhatsApp, Instagram, etc.)")
    @ApiResponse(responseCode = "201", description = "Canal registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ChannelConnectionResponseDto> create(
            @Valid @RequestBody CreateChannelConnectionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelConnectionService.create(dto));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar canal")
    @ApiResponse(responseCode = "200", description = "Canal activado")
    @ApiResponse(responseCode = "400", description = "Canal no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ChannelConnectionResponseDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(channelConnectionService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar canal")
    @ApiResponse(responseCode = "200", description = "Canal desactivado")
    public ResponseEntity<ChannelConnectionResponseDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(channelConnectionService.deactivate(id));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar canales de una empresa")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<ChannelConnectionResponseDto>> findByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(channelConnectionService.findByCompany(companyId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar canal")
    @ApiResponse(responseCode = "204", description = "Canal eliminado")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        channelConnectionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar canal",
            description = "Actualiza token, verify token o URL del webhook. Si se envía webhookBaseUrl, re-registra el webhook en Telegram y activa el canal.")
    @ApiResponse(responseCode = "200", description = "Canal actualizado")
    @ApiResponse(responseCode = "404", description = "Canal no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ChannelConnectionResponseDto> update(
            @PathVariable Long id,
            @RequestBody UpdateChannelConnectionRequestDto dto) {
        return ResponseEntity.ok(channelConnectionService.update(id, dto));
    }

    @GetMapping("/{id}/webhook-info")
    @Operation(summary = "Verificar estado del webhook en Telegram",
            description = "Consulta a Telegram el estado actual del webhook registrado para este canal")
    @ApiResponse(responseCode = "200", description = "Información del webhook")
    @ApiResponse(responseCode = "400", description = "Canal no es de tipo TELEGRAM",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Canal no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<WebhookInfoDto> getWebhookInfo(@PathVariable Long id) {
        return ResponseEntity.ok(channelConnectionService.getWebhookInfo(id));
    }
}

