package com.iclinic.iclinicbackend.shared.tenant;

import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resuelve sobre qué empresa opera la petición y lo deja en {@link TenantContext}.
 * <p>
 * Corre después de la autenticación, así que ve al usuario ya resuelto por
 * {@code TokenRevocationFilter}. <strong>Limpia siempre en un {@code finally}</strong>:
 * el hilo vuelve al pool del contenedor y la siguiente petición heredaría el
 * tenant de ésta.
 * <p>
 * <b>El puente, y por qué está aquí.</b> Hoy <em>ninguna ruta de producción crea
 * una {@code CompanyMembership}</em>: el único sitio que las escribe es el
 * backfill de arranque. Es decir, un usuario creado por {@code POST /api/v1/users}
 * no tiene membresía hasta el siguiente reinicio. Eso funciona por accidente,
 * porque la autorización todavía mira {@code users.company_id} — y este bloque es
 * justo el que empieza a cambiarlo.
 * <p>
 * Si aquí resolviéramos el tenant solo desde la membresía, todos esos usuarios
 * recibirían 403 en todo. Así que se cae a {@code users.company_id} con un
 * {@code WARN} que los nombra: ese log es el inventario de quién está en el estado
 * malo. El paso 2 —y este comentario— desaparecen en el PR que crea la membresía
 * en las rutas de alta.
 */
@RequiredArgsConstructor
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private final CompanyMembershipRepository membershipRepository;
    private final com.iclinic.iclinicbackend.modules.user.repository.UserRepository userRepository;

    /** Rutas públicas: sin autenticar no hay tenant que resolver. */
    private static final List<String> SIN_TENANT = List.of(
            "/api/v1/crm/webhooks", "/swagger-ui", "/api-docs", "/v3/api-docs",
            "/actuator/health", "/ws-stomp", "/ws");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            resolver().ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            // Sin esto, el siguiente que use este hilo ve la clínica anterior.
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return SIN_TENANT.stream().anyMatch(ruta::startsWith);
    }

    private java.util.Optional<Long> resolver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)
                || !(jwtAuth.getDetails() instanceof User user)) {
            return java.util.Optional.empty();
        }

        // 1) El camino bueno: la membresía activa.
        var membresias = membershipRepository.findByUserIdAndActiveTrue(user.getId());
        if (!membresias.isEmpty()) {
            if (membresias.size() > 1) {
                // Multi-empresa llega con el bloque D, junto a X-Organization-Id.
                log.debug("userId={} pertenece a {} empresas; se toma la primera",
                        user.getId(), membresias.size());
            }
            return java.util.Optional.of(membresias.get(0).getCompany().getId());
        }

        // 2) Puente temporal. Ver el javadoc: se retira con la ruta de alta.
        //    Se consulta el id, no user.getCompany(): el User viene desatachado
        //    del filtro anterior y la relacion perezosa estallaria con un 500.
        var empresaHeredada = userRepository.findCompanyIdByUserId(user.getId());
        if (empresaHeredada.isPresent()) {
            log.warn("PUENTE DE TENANT: userId={} ({}) no tiene membresia activa; "
                            + "se usa users.company_id={}. Este usuario quedara sin acceso "
                            + "cuando se retire el puente.",
                    user.getId(), user.getEmail(), empresaHeredada.get());
            return empresaHeredada;
        }

        // 3) Ni membresía ni empresa. Un administrador de plataforma entra aquí, y
        //    es correcto: no opera sobre ninguna clínica concreta.
        return java.util.Optional.empty();
    }
}
