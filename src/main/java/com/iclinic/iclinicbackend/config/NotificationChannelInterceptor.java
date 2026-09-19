package com.iclinic.iclinicbackend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelInterceptor implements ChannelInterceptor {
    private final ObjectProvider<FirebaseAuth> firebaseAuth;
    private final UserRepository users;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) throw new AccessDeniedException("STOMP required");
        if (headers.getCommand() == StompCommand.CONNECT) {
            String authorization = headers.getFirstNativeHeader("Authorization");
            FirebaseAuth auth = firebaseAuth.getIfAvailable();
            if (auth == null || authorization == null || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("Authentication required");
            }
            try {
                String uid = auth.verifyIdToken(authorization.substring(7)).getUid();
                var user = users.findByExternalAuthId(uid)
                        .orElseThrow(() -> new AccessDeniedException("Unknown user"));
                if (!Boolean.TRUE.equals(user.getActive())) throw new AccessDeniedException("Inactive user");
                headers.setUser(() -> uid);
            } catch (FirebaseAuthException | IllegalArgumentException ex) {
                throw new AccessDeniedException("Invalid Firebase token");
            }
        } else if (headers.getCommand() == StompCommand.SUBSCRIBE) {
            if (headers.getUser() == null) throw new AccessDeniedException("Authentication required");
            var user = users.findByExternalAuthId(headers.getUser().getName())
                    .orElseThrow(() -> new AccessDeniedException("Unknown user"));
            String destination = headers.getDestination();
            boolean permittedTopic = destination != null && destination.matches("/topic/notifications/[1-9][0-9]*");
            boolean permittedCompany = user.getRole() == UserRole.SUPER_ADMIN
                    || (user.getCompany() != null && ("/topic/notifications/" + user.getCompany().getId()).equals(destination));
            if (!Boolean.TRUE.equals(user.getActive()) || !permittedTopic || !permittedCompany
                    || user.getRole() == UserRole.PATIENT || user.getRole() == UserRole.EXTERNAL_DOCTOR) {
                throw new AccessDeniedException("Subscription not permitted");
            }
        } else if (headers.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Client publishing not permitted");
        }
        return message;
    }
}
