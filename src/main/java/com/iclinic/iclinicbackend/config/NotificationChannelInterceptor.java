package com.iclinic.iclinicbackend.config;

import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.SubjectType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Autentica el CONNECT de STOMP con el mismo token de Keycloak que la API REST, y
 * autoriza cada suscripción por separado.
 * <p>
 * La cadena de filtros HTTP no cubre los fotogramas de STOMP: el WebSocket se
 * abre una vez y después viajan mensajes por dentro. Por eso aquí se repite la
 * validación del token, incluido el kill-switch de {@code tokens_valid_from}.
 */
@Component
@RequiredArgsConstructor
public class NotificationChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final UserRepository users;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) throw new AccessDeniedException("STOMP required");

        if (headers.getCommand() == StompCommand.CONNECT) {
            User user = authenticate(headers.getFirstNativeHeader("Authorization"));
            String subject = user.getKeycloakUserId().toString();
            // Las cabeceras del mensaje recibido pueden venir ya congeladas, así
            // que se envuelven en un accesor mutable y se reconstruye el mensaje
            // en lugar de mutar el original.
            StompHeaderAccessor mutables = StompHeaderAccessor.wrap(message);
            mutables.setUser(() -> subject);
            return MessageBuilder.createMessage(message.getPayload(), mutables.getMessageHeaders());

        } else if (headers.getCommand() == StompCommand.SUBSCRIBE) {
            if (headers.getUser() == null) throw new AccessDeniedException("Authentication required");
            User user = users.findByKeycloakUserId(parseSubject(headers.getUser().getName()))
                    .orElseThrow(() -> new AccessDeniedException("Unknown user"));

            String destination = headers.getDestination();
            boolean permittedTopic = destination != null
                    && destination.matches("/topic/notifications/[1-9][0-9]*");
            boolean permittedCompany = user.getRole() == UserRole.SUPER_ADMIN
                    || (user.getCompany() != null
                        && ("/topic/notifications/" + user.getCompany().getId()).equals(destination));

            if (!Boolean.TRUE.equals(user.getActive()) || !permittedTopic || !permittedCompany
                    || user.getRole() == UserRole.PATIENT
                    || user.getRole() == UserRole.EXTERNAL_DOCTOR) {
                throw new AccessDeniedException("Subscription not permitted");
            }

        } else if (headers.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Client publishing not permitted");
        }

        return message;
    }

    private User authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authentication required");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(authorization.substring(7));
        } catch (JwtException ex) {
            throw new AccessDeniedException("Invalid token");
        }

        User user = users.findByKeycloakUserId(parseSubject(jwt.getSubject()))
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException("Inactive user");
        }
        // Una cuenta de servicio no se suscribe a la bandeja de nadie.
        if (user.getSubjectType() == SubjectType.SERVICE_ACCOUNT) {
            throw new AccessDeniedException("Service accounts cannot subscribe");
        }
        Instant issuedAt = jwt.getIssuedAt();
        if (issuedAt != null && issuedAt.isBefore(user.getTokensValidFrom())) {
            throw new AccessDeniedException("Revoked session");
        }
        return user;
    }

    private UUID parseSubject(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AccessDeniedException("Invalid subject");
        }
    }
}
