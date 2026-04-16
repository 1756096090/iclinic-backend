package com.iclinic.iclinicbackend.modules.crm.webhook.controller;

import com.iclinic.iclinicbackend.modules.crm.webhook.service.MetaWebhookService;
import com.iclinic.iclinicbackend.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crm/webhooks/meta")
@RequiredArgsConstructor
@Tag(name = "CRM Webhooks", description = "Endpoints de webhook para Meta (WhatsApp, Instagram, Facebook)")
public class MetaWebhookController {

    private final MetaWebhookService metaWebhookService;

    @GetMapping
    @Operation(summary = "Verificar webhook de Meta",
            description = "Endpoint de verificación que Meta llama al registrar el webhook")
    @ApiResponse(responseCode = "200", description = "Challenge retornado")
    @ApiResponse(responseCode = "400", description = "Token inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        return ResponseEntity.ok(metaWebhookService.verifyWebhook(mode, token, challenge));
    }

    @PostMapping
    @Operation(summary = "Recibir eventos de Meta",
            description = "Meta envía mensajes entrantes y actualizaciones de estado aquí")
    @ApiResponse(responseCode = "200", description = "Evento procesado")
    public ResponseEntity<Void> receive(@RequestBody String payload) {
        metaWebhookService.processIncomingPayload(payload);
        return ResponseEntity.ok().build();
    }
}

