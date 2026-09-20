package com.iclinic.iclinicbackend.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Comprueba que el token fue emitido PARA esta API.
 * <p>
 * Sin esta validación, cualquier token firmado por el mismo realm sirve para
 * entrar aquí: el de la aplicación móvil, el de una herramienta interna, el de
 * cualquier cliente que alguien dé de alta. El validador por defecto solo
 * comprueba el emisor, y el emisor es el mismo para todos los clientes del realm.
 */
@RequiredArgsConstructor
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR = new OAuth2Error(
            "invalid_token",
            "El token no fue emitido para esta API",
            null);

    private final String expectedAudience;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience != null && audience.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(ERROR);
    }
}
