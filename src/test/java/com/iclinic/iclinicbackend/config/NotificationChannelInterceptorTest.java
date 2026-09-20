package com.iclinic.iclinicbackend.config;

import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.SubjectType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationChannelInterceptorTest {

    private static final UUID SUB = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UserRepository users = mock(UserRepository.class);
    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final NotificationChannelInterceptor interceptor =
            new NotificationChannelInterceptor(jwtDecoder, users);

    @Test
    void rechazaConexionesSuscripcionesYEnviosSinAutenticar() {
        for (var command : new StompCommand[]{StompCommand.CONNECT, StompCommand.SUBSCRIBE, StompCommand.SEND}) {
            var headers = StompHeaderAccessor.create(command);
            var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            assertThatThrownBy(() -> interceptor.preSend(message, null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void soloElPersonalActivoSeSuscribeALaBandejaDeSuEmpresa() {
        var user = usuario(UserRole.ADMIN, 1L);
        when(users.findByKeycloakUserId(SUB)).thenReturn(Optional.of(user));

        var permitido = suscripcion("/topic/notifications/1");
        assertThat(interceptor.preSend(permitido, null)).isSameAs(permitido);

        var ajena = suscripcion("/topic/notifications/2");
        assertThatThrownBy(() -> interceptor.preSend(ajena, null))
                .isInstanceOf(AccessDeniedException.class);

        user.setActive(false);
        assertThatThrownBy(() -> interceptor.preSend(permitido, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unTokenInvalidoNoAbreLaConexion() {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("firma no válida"));

        assertThatThrownBy(() -> interceptor.preSend(conexion("Bearer basura"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** El kill-switch de {@code tokens_valid_from} también cubre el WebSocket. */
    @Test
    void unTokenEmitidoAntesDeLaRevocacionNoAbreLaConexion() {
        var user = usuario(UserRole.ADMIN, 1L);
        user.setTokensValidFrom(Instant.parse("2026-09-10T00:00:00Z"));
        when(users.findByKeycloakUserId(SUB)).thenReturn(Optional.of(user));
        when(jwtDecoder.decode(anyString())).thenReturn(jwt(Instant.parse("2026-09-09T23:59:00Z")));

        assertThatThrownBy(() -> interceptor.preSend(conexion("Bearer viejo"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unTokenPosteriorALaRevocacionSiAbreLaConexion() {
        var user = usuario(UserRole.ADMIN, 1L);
        user.setTokensValidFrom(Instant.parse("2026-09-10T00:00:00Z"));
        when(users.findByKeycloakUserId(SUB)).thenReturn(Optional.of(user));
        when(jwtDecoder.decode(anyString())).thenReturn(jwt(Instant.parse("2026-09-10T00:01:00Z")));

        // El CONNECT reconstruye el mensaje para poder fijar el principal, asi que
        // lo que se comprueba es que salga autenticado, no que sea el mismo objeto.
        var autenticado = interceptor.preSend(conexion("Bearer nuevo"), null);
        var cabeceras = StompHeaderAccessor.wrap(autenticado);
        assertThat(cabeceras.getUser()).isNotNull();
        assertThat(cabeceras.getUser().getName()).isEqualTo(SUB.toString());
    }

    /** Una integración no se suscribe a la bandeja de una clínica. */
    @Test
    void unaCuentaDeServicioNoSeSuscribe() {
        var agente = usuario(UserRole.ADMIN, 1L);
        agente.setSubjectType(SubjectType.SERVICE_ACCOUNT);
        when(users.findByKeycloakUserId(SUB)).thenReturn(Optional.of(agente));
        when(jwtDecoder.decode(anyString())).thenReturn(jwt(Instant.now()));

        assertThatThrownBy(() -> interceptor.preSend(conexion("Bearer maquina"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- utilidades ----------

    private EcuadorianUser usuario(UserRole role, Long companyId) {
        var company = new EcuadorianCompany();
        company.setId(companyId);
        var user = new EcuadorianUser();
        user.setActive(true);
        user.setRole(role);
        user.setCompany(company);
        user.setKeycloakUserId(SUB);
        user.setSubjectType(SubjectType.HUMAN);
        user.setTokensValidFrom(Instant.EPOCH);
        return user;
    }

    private Jwt jwt(Instant issuedAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(SUB.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();
    }

    private org.springframework.messaging.Message<byte[]> conexion(String authorization) {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.addNativeHeader("Authorization", authorization);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private org.springframework.messaging.Message<byte[]> suscripcion(String destino) {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setUser(SUB::toString);
        headers.setDestination(destino);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
