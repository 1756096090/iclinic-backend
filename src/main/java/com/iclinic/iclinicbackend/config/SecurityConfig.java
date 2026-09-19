package com.iclinic.iclinicbackend.config;

import com.iclinic.iclinicbackend.modules.auth.filter.TokenRevocationFilter;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.security.AudienceValidator;
import com.iclinic.iclinicbackend.shared.security.KeycloakRolesConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final UserRepository userRepository;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String clientId;

    public SecurityConfig(
            UserRepository userRepository,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${iclinic.keycloak.client-id}") String clientId) {
        this.userRepository = userRepository;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri.isBlank()
                ? issuerUri + "/protocol/openid-connect/certs"
                : jwkSetUri;
        this.clientId = clientId;
    }

    /**
     * Valida firma, emisor y <strong>audiencia</strong>. Lo tercero no lo hace
     * Spring por defecto: sin ello, un token emitido para otro cliente del mismo
     * realm entra aquí como si fuera propio.
     * <p>
     * Se configura el JWKS <em>directamente</em>, sin descubrimiento OIDC. Tanto
     * {@code JwtDecoders.fromIssuerLocation} como
     * {@code NimbusJwtDecoder.withIssuerLocation} llaman a
     * {@code /.well-known/openid-configuration} al crear el bean, y eso ata el
     * arranque de la API a que Keycloak esté en pie en ese instante: un reinicio
     * durante una ventana de mantenimiento del servidor de identidad dejaría la
     * API abajo hasta que alguien volviera a reiniciarla. Con el JWKS explícito,
     * la única llamada de red ocurre al validar el primer token, y se reintenta.
     * <p>
     * La ruta del JWKS de Keycloak es estable, así que fijarla no pierde nada; aun
     * así se puede sobrescribir por propiedad para otro proveedor.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator(clientId));

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public TokenRevocationFilter tokenRevocationFilter() {
        return new TokenRevocationFilter(userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            // Un servidor de recursos no guarda sesión: cada petición trae su token.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRolesConverter(clientId))))
            .authorizeHttpRequests(auth -> auth
                // ── Públicos ──
                // La autenticidad de los webhooks depende de la firma o del secreto
                // del proveedor, no de la red. Ver crm/webhook.
                .requestMatchers("/api/v1/crm/webhooks/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                 "/api-docs/**", "/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // La autenticación del WebSocket se comprueba en el CONNECT de STOMP
                // y cada suscripción se autoriza aparte. Ver NotificationChannelInterceptor.
                .requestMatchers("/ws-stomp", "/ws/**").permitAll()

                // ── Autenticado ──
                .requestMatchers("/api/v1/auth/me").authenticated()

                // ── Solo SUPER_ADMIN ──
                .requestMatchers("/api/v1/admin/super-admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/client-onboarding").hasRole("SUPER_ADMIN")

                // ── SUPER_ADMIN o ADMIN ──
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // ── Usuarios: lectura para personal, mutación para administración ──
                .requestMatchers(HttpMethod.POST,   "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/v1/users/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER", "DENTIST",
                                "ASSISTANT", "RECEPTIONIST")

                // ── Acceso de doctor externo ──
                .requestMatchers("/api/v1/external-doctor-access/my-patients")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER", "DENTIST",
                                "ASSISTANT", "RECEPTIONIST", "EXTERNAL_DOCTOR")
                .requestMatchers("/api/v1/external-doctor-access/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "DENTIST")

                // ── Empresas y sucursales ──
                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**", "/api/v1/branches/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER", "DENTIST",
                                "ASSISTANT", "RECEPTIONIST", "BILLING")
                .requestMatchers("/api/v1/companies/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/v1/branches/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER")

                // ── Agenda ──
                .requestMatchers("/api/v1/appointments/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER", "DENTIST",
                                "ASSISTANT", "RECEPTIONIST")

                // ── CRM ──
                .requestMatchers("/api/v1/crm/channels/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/v1/crm/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER",
                                "RECEPTIONIST", "ASSISTANT", "DENTIST")

                // ── Todo lo demás se deniega ──
                // denyAll() y no authenticated(): un controlador nuevo sin regla
                // debe dar 403, no quedar abierto a cualquier usuario con token.
                .anyRequest().denyAll()
            )
            .addFilterAfter(tokenRevocationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.trycloudflare.com",
                "https://*.ngrok-free.app",
                "https://*.devtunnels.ms"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
