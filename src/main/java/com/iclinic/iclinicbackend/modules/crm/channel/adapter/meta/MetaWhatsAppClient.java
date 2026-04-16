package com.iclinic.iclinicbackend.modules.crm.channel.adapter.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para la Meta Cloud API (WhatsApp Business).
 *
 * <p>POST https://graph.facebook.com/v18.0/{phoneNumberId}/messages</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetaWhatsAppClient {

    private static final String META_API_BASE = "https://graph.facebook.com/v18.0";

    private final RestTemplate restTemplate;

    /**
     * Envía un mensaje de texto a través de la Meta Cloud API.
     *
     * @param accessToken     Token de acceso de la cuenta de WhatsApp Business
     * @param phoneNumberId   ID del número de teléfono emisor (de la cuenta Meta)
     * @param toPhone         Número de destino en formato internacional (ej: 593987654321)
     * @param text            Cuerpo del mensaje
     * @return wamid (WhatsApp Message ID) devuelto por Meta
     */
    public String sendTextMessage(String accessToken, String phoneNumberId, String toPhone, String text) {
        String url = META_API_BASE + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toPhone,
                "type", "text",
                "text", Map.of("body", text)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            MetaSendMessageResponse response = restTemplate.postForObject(url, request, MetaSendMessageResponse.class);

            if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
                throw new RuntimeException("Meta API devolvió respuesta vacía al enviar mensaje");
            }

            String wamid = response.getMessages().get(0).getId();
            log.info("WhatsApp message sent: wamid={} to={}", wamid, toPhone);
            return wamid;
        } catch (Exception e) {
            log.error("Error llamando a Meta Cloud API: url={} to={}", url, toPhone, e);
            throw new RuntimeException("Error enviando mensaje de WhatsApp vía Meta: " + e.getMessage(), e);
        }
    }

    // ─── response DTOs ──────────────────────────────────────────────────────────

    static class MetaSendMessageResponse {
        private List<WaMessage> messages;

        public List<WaMessage> getMessages() { return messages; }

        public void setMessages(List<WaMessage> messages) { this.messages = messages; }
    }

    static class WaMessage {
        private String id;

        @JsonProperty("message_status")
        private String messageStatus;

        public String getId() { return id; }

        public void setId(String id) { this.id = id; }
    }
}
