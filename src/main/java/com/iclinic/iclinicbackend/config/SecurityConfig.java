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
                .requestMatchers("/api/v1/auth/firebase/sync").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/crm/webhooks/**").permitAll()
                .requestMatchers("/api/v1/admin/client-onboarding").permitAll()
                .requestMatchers("/api/v1/admin/users/invite").permitAll()
                .requestMatchers("/api/v1/external-doctor-access/**").permitAll()
                .requestMatchers("/api/v1/external-doctor-access/my-patients",
                                 "/api/v1/external-doctor-access/patients/**").permitAll()
                .requestMatchers("/api/v1/companies/**").permitAll()
                .requestMatchers("/api/v1/branches/**").permitAll()
                .requestMatchers("/api/v1/users/**").permitAll()
                .requestMatchers("/api/v1/crm/channels/**").permitAll()
                .requestMatchers("/api/v1/crm/conversations/**").permitAll()
                .requestMatchers("/api/v1/crm/messages/**").permitAll()
                .requestMatchers("/api/v1/appointments/**").permitAll()
                .anyRequest().permitAll()
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

