package com.iclinic.iclinicbackend.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Traduce los <em>client roles</em> de Keycloak a autoridades de Spring Security.
 * <p>
 * Keycloak los publica anidados en {@code resource_access.<cliente>.roles}; el
 * conversor por defecto de Spring lee {@code scope}, que es otra cosa, así que
 * sin esto el token llega sin ninguna autoridad y todo responde 403.
 * <p>
 * Se leen únicamente los roles del cliente propio: los <em>realm roles</em> se
 * ignoran a propósito, porque los comparte cualquier cliente del realm y no
 * deberían conceder nada aquí.
 * <p>
 * Estas autoridades dicen qué rol <em>tiene</em> el sujeto, no sobre qué puede
 * ejercerlo. El rol efectivo es la intersección de esto con la membresía en la
 * empresa, y esa parte vive en el bloque D.
 */
@RequiredArgsConstructor
public class KeycloakRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";

    private final String clientId;

    @Override
    @SuppressWarnings("unchecked")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess != null && resourceAccess.get(clientId) instanceof Map<?, ?> client
                && client.get(ROLES) instanceof Collection<?> roles) {
            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
        }

        return new JwtAuthenticationToken(jwt, List.copyOf(authorities), jwt.getSubject());
    }
}
