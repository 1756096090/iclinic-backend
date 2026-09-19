package com.iclinic.iclinicbackend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationChannelInterceptorTest {
    private final UserRepository users = mock(UserRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<FirebaseAuth> firebase = mock(ObjectProvider.class);
    private final NotificationChannelInterceptor interceptor = new NotificationChannelInterceptor(firebase, users);

    @Test
    void rejectsUnauthenticatedConnectionsAndSubscriptions() {
        for (var command : new StompCommand[]{StompCommand.CONNECT, StompCommand.SUBSCRIBE, StompCommand.SEND}) {
            var headers = StompHeaderAccessor.create(command);
            var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            assertThatThrownBy(() -> interceptor.preSend(message, null)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void onlyAllowsActiveStaffToSubscribeToTheirCompany() {
        var company = new EcuadorianCompany();
        company.setId(1L);
        var user = new EcuadorianUser();
        user.setActive(true);
        user.setRole(UserRole.ADMIN);
        user.setCompany(company);
        when(users.findByExternalAuthId("uid")).thenReturn(Optional.of(user));
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setUser(() -> "uid");
        headers.setDestination("/topic/notifications/1");
        var allowed = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
        assertThat(interceptor.preSend(allowed, null)).isSameAs(allowed);
        var other = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        other.setUser(() -> "uid");
        other.setDestination("/topic/notifications/2");
        var denied = MessageBuilder.createMessage(new byte[0], other.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(denied, null)).isInstanceOf(AccessDeniedException.class);
        user.setActive(false);
        assertThatThrownBy(() -> interceptor.preSend(allowed, null)).isInstanceOf(AccessDeniedException.class);
    }
}
