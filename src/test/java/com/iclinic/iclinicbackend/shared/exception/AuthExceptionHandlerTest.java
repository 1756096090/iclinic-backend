package com.iclinic.iclinicbackend.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sucesor de {@code FirebaseAuthExceptionHandlerTest}.
 * <p>
 * El comportamiento que aquel test protegía sigue siendo necesario: un fallo de
 * autenticación tiene que salir por el contrato {@code ErrorResponse} con el
 * código correcto, y no como una traza de 500. Lo que cambia es la excepción de
 * origen: con Keycloak ya no hay {@code FirebaseAuthException}, sino
 * {@link JwtException} para el token rechazado y {@link AccessDeniedException}
 * para el permiso insuficiente.
 * <p>
 * La distinción entre 401 y 403 importa: un token inválido se arregla volviendo a
 * iniciar sesión, y un permiso insuficiente no.
 */
class AuthExceptionHandlerTest {

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/api/v1/prueba/token-invalido")
        String tokenInvalido() {
            throw new BadJwtException("firma no válida");
        }

        @GetMapping("/api/v1/prueba/sin-permiso")
        String sinPermiso() {
            throw new AccessDeniedException("falta el rol");
        }
    }

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void tokenRechazadoDevuelve401ConElContratoDeError() throws Exception {
        mvc.perform(get("/api/v1/prueba/token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.details").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.path").value("/api/v1/prueba/token-invalido"));
    }

    @Test
    void permisoInsuficienteDevuelve403YNoSeConfundeCon401() throws Exception {
        mvc.perform(get("/api/v1/prueba/sin-permiso"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.details").value("ACCESS_DENIED"));
    }

    @Test
    void elMensajeDeErrorNoFiltraElDetalleInternoDelToken() throws Exception {
        mvc.perform(get("/api/v1/prueba/token-invalido"))
                .andExpect(jsonPath("$.message").value("Credenciales no validas. Vuelve a iniciar sesion."));
    }
}
