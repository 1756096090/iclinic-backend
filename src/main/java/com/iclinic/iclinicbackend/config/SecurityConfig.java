package com.iclinic.iclinicbackend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.iclinic.iclinicbackend.modules.auth.filter.FirebaseAuthenticationFilter;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {
    private final Optional<FirebaseAuth> firebaseAuth;
    private final UserRepository userRepository;

    public SecurityConfig(Optional<FirebaseAuth> firebaseAuth, UserRepository userRepository) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
        if (firebaseAuth.isEmpty()) {
            log.warn("FirebaseAuth is not available - Firebase authentication disabled");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnBean(FirebaseAuth.class)
    public FirebaseAuthenticationFilter firebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        return new FirebaseAuthenticationFilter(firebaseAuth, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ── Públicos: no requieren autenticación ──
                .requestMatchers("/api/v1/auth/firebase/sync").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/crm/webhooks/**").permitAll()

                // ── Requieren autenticación ──
                .requestMatchers("/api/v1/auth/me").authenticated()

                // ── Solo SUPER_ADMIN ──
                .requestMatchers("/api/v1/admin/super-admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/client-onboarding").hasRole("SUPER_ADMIN")

                // ── SUPER_ADMIN o ADMIN ──
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // ── Gestión de usuarios: lectura para staff autenticado (el servicio
                //    filtra por empresa), pero mutaciones sólo ADMIN/SUPER_ADMIN ──
                .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // ── Acceso de doctor externo ──
                .requestMatchers("/api/v1/external-doctor-access/my-patients")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "DENTIST", "ASSISTANT", "RECEPTIONIST", "EXTERNAL_DOCTOR")
                .requestMatchers("/api/v1/external-doctor-access/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "DENTIST")

                // ── El resto requiere cualquier usuario autenticado ──
                .anyRequest().authenticated()
            );

        // Add Firebase filter only if available
        if (firebaseAuth.isPresent()) {
            http.addFilterBefore(
                firebaseAuthenticationFilter(firebaseAuth.get()),
                UsernamePasswordAuthenticationFilter.class
            );
        }

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

