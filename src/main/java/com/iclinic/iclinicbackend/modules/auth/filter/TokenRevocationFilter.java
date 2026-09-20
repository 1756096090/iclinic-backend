package com.iclinic.iclinicbackend.modules.auth.filter;

import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Corta las sesiones vivas sin esperar a que caduque el token.
 * <p>
 * Keycloak valida la firma, pero un token ya emitido sigue siendo válido hasta
 * que expira: al dar de baja a alguien, seguiría entrando durante los minutos que
 * le queden. La alternativa habitual es una lista de revocación en el backend, que
 * obliga a guardar estado de sesión. Aquí basta con una marca por usuario: si el
 * token se emitió antes de {@code tokens_valid_from}, se rechaza.
 * <p>
 * Un {@code UPDATE users SET tokens_valid_from = now()} invalida todas las
 * sesiones de esa persona en la siguiente petición.
 * <p>
 * Se ejecuta después de la validación del JWT, así que solo ve tokens ya
 * verificados. También resuelve la proyección local del sujeto y la deja en los
 * detalles de la autenticación, para que {@code CurrentUserService} no repita la
 * consulta.
 */
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            Optional<UUID> subject = parseSubject(jwt.getSubject());
            if (subject.isEmpty()) {
                reject(response, "El token no trae un sujeto válido");
                return;
            }

            Optional<User> found = userRepository.findByKeycloakUserId(subject.get());
            if (found.isEmpty()) {
                // Autenticado en Keycloak pero sin proyección local: no puede operar
                // sobre el dominio. Ocurre entre el alta en Keycloak y la sincronización.
                reject(response, "El usuario no está dado de alta en esta instalación");
                return;
            }

            User user = found.get();

            if (!Boolean.TRUE.equals(user.getActive())) {
                reject(response, "El usuario está inactivo");
                return;
            }

            Instant issuedAt = jwt.getIssuedAt();
            if (issuedAt != null && issuedAt.isBefore(user.getTokensValidFrom())) {
                log.info("Token revocado para userId={}: iat={} anterior a tokensValidFrom={}",
                        user.getId(), issuedAt, user.getTokensValidFrom());
                reject(response, "La sesión fue revocada. Vuelve a iniciar sesión.");
                return;
            }

            jwtAuth.setDetails(user);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<UUID> parseSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private void reject(HttpServletResponse response, String motivo) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + motivo + "\"}");
    }
}
