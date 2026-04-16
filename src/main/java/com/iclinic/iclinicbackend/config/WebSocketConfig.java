package com.iclinic.iclinicbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración WebSocket (STOMP) para notificaciones en tiempo real.
 *
 * <h3>Endpoints que expone:</h3>
 * <ul>
 *   <li>{@code /ws} — handshake WebSocket con fallback SockJS para
 *       navegadores que no soportan WebSocket nativo</li>
 * </ul>
 *
 * <h3>Tópicos que el frontend puede suscribirse:</h3>
 * <ul>
 *   <li>{@code /topic/notifications/{companyId}} — notificaciones de nuevos
 *       mensajes entrantes (campanita). Payload: {@link com.iclinic.iclinicbackend.modules.crm.message.dto.MessageNotificationDto}</li>
 * </ul>
 *
 * <h3>Ejemplo de uso desde Angular/React (con @stomp/stompjs):</h3>
 * <pre>{@code
 * const client = new Client({
 *   brokerURL: 'ws://localhost:8080/ws/websocket',
 *   // O con SockJS:
 *   webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
 * });
 *
 * client.onConnect = () => {
 *   client.subscribe('/topic/notifications/1', message => {
 *     const notification = JSON.parse(message.body);
 *     showBellNotification(notification);
 *   });
 * };
 *
 * client.activate();
 * }</pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en memoria para los tópicos de notificación
        registry.enableSimpleBroker("/topic");
        // Prefijo para mensajes que el cliente envía al servidor (no usados aún)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint — compatible con Cloudflare Tunnel y @stomp/stompjs
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://*.devtunnels.ms",
                        "https://*.ngrok-free.app",
                        "https://*.ngrok.io",
                        "https://*.trycloudflare.com",
                        "https://*.cloudflareaccess.com"
                );

        // SockJS endpoint — fallback para clientes legacy
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://*.devtunnels.ms",
                        "https://*.ngrok-free.app",
                        "https://*.ngrok.io",
                        "https://*.trycloudflare.com",
                        "https://*.cloudflareaccess.com"
                )
                .withSockJS(); // fallback para navegadores sin WebSocket nativo
    }
}

