package com.iclinic.iclinicbackend.modules.crm.message.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iclinic.iclinicbackend.modules.crm.message.dto.NewMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventPublisher {

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    private final ObjectMapper objectMapper;

    private final Map<Long, List<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long companyId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        List<SseEmitter> emitters = emittersByCompany
                .computeIfAbsent(companyId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        log.info("SSE client subscribed: companyId={} totalClients={}", companyId, emitters.size());

        Runnable cleanup = () -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected: companyId={} remaining={}", companyId, emitters.size());
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"companyId\":" + companyId + "}"));
        } catch (IOException e) {
            log.warn("Could not send 'connected' event to new SSE client: {}", e.getMessage());
        }

        return emitter;
    }

    public void publish(NewMessageEvent event) {
        List<SseEmitter> emitters = emittersByCompany.get(event.getCompanyId());
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE clients for companyId={}, skipping push", event.getCompanyId());
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize NewMessageEvent: {}", e.getMessage());
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("new_message").data(json));
            } catch (IOException e) {
                log.warn("SSE emitter dead, queuing for removal: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);

        log.info("SSE event 'new_message' pushed: companyId={} clients={} messageId={}",
                event.getCompanyId(), emitters.size(), event.getMessageId());
    }
}

