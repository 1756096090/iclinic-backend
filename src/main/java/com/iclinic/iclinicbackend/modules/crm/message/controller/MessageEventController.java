package com.iclinic.iclinicbackend.modules.crm.message.controller;

import com.iclinic.iclinicbackend.modules.crm.message.service.MessageEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Endpoint SSE para que el frontend reciba mensajes entrantes en tiempo real.
 *
 * <p>El frontend se suscribe una vez con:
 * <pre>
 *   GET /api/v1/crm/events/messages?companyId=1
 *   Accept: text/event-stream
 * </pre>
 * </p>
 *
 * <p>Eventos que puede recibir:
 * <ul>
 *   <li>{@code connected} — confirmación de suscripción</li>
 *   <li>{@code new_message} — JSON de {@link com.iclinic.iclinicbackend.modules.crm.message.dto.NewMessageEvent}</li>
 * </ul>
 * </p>
 *
 * <p>El frontend debe reconectar automáticamente cuando el emitter expire
 * (timeout de 5 minutos). EventSource de la API de navegador lo hace nativo.</p>
 */
@RestController
@RequestMapping("/api/v1/crm/events")
@RequiredArgsConstructor
@Tag(name = "CRM Events (SSE)", description = "Server-Sent Events para tiempo real")
public class MessageEventController {

    private final MessageEventPublisher eventPublisher;

    /**
     * Abre un stream SSE para recibir eventos de nuevos mensajes entrantes.
     *
     * <p>Uso desde JavaScript/TypeScript:
     * <pre>{@code
     * const source = new EventSource(
     *   'http://localhost:8080/api/v1/crm/events/messages?companyId=1',
     *   { withCredentials: true }
     * );
     * source.addEventListener('new_message', e => {
     *   const msg = JSON.parse(e.data);
     *   console.log('Nuevo mensaje:', msg);
     * });
     * source.addEventListener('connected', e => console.log('SSE conectado', e.data));
     * source.onerror = () => console.error('SSE desconectado, reconectando...');
     * }</pre>
     * </p>
     *
     * @param companyId ID de la empresa cuyos mensajes quiere recibir el cliente
     * @return SSE stream (mantiene la conexión abierta)
     */
    @GetMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Suscribirse a mensajes en tiempo real (SSE)",
            description = "Abre un stream que envía 'new_message' cada vez que llega un mensaje entrante de Telegram, WhatsApp, etc."
    )
    public SseEmitter subscribe(
            @Parameter(description = "ID de la empresa", required = true)
            @RequestParam Long companyId) {
        return eventPublisher.subscribe(companyId);
    }
}

