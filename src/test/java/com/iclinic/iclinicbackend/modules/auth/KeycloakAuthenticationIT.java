package com.iclinic.iclinicbackend.modules.auth;

import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.SubjectType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criterios de aceptación del bloque A, contra un Keycloak real.
 * <p>
 * No sirve simular el decodificador: lo que hay que demostrar es que un token
 * emitido de verdad por Keycloak entra, y que los tres casos que deben
 * rechazarse se rechazan. Dos de ellos —la audiencia y el kill-switch— son
 * configuración nuestra, no de Spring, y un test con mocks los daría por buenos
 * aunque estuvieran mal montados.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakAuthenticationIT extends AbstractPostgresIT {

    private static final String REALM = "iclinic";
    private static final String API_CLIENT = "iclinic-api";

    /**
     * Imagen oficial con un {@link GenericContainer}, en lugar de la libreria
     * {@code testcontainers-keycloak}: su matriz de compatibilidad va por detras
     * de las versiones de Keycloak y su estrategia de espera apunta a
     * {@code /health} en el puerto 8080, cuando desde Keycloak 25 la salud vive
     * en la interfaz de gestion (9000). Aqui se espera contra el endpoint que de
     * verdad importa, el realm respondiendo, y no hay version de libreria que
     * sincronizar.
     */
    @SuppressWarnings("resource")
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
                    .withExposedPorts(8080)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/realm-iclinic-test.json"),
                            "/opt/keycloak/data/import/realm-iclinic-test.json")
                    .withCommand("start-dev", "--import-realm")
                    .waitingFor(Wait.forHttp("/realms/" + REALM)
                            .forPort(8080)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static {
        KEYCLOAK.start();
    }

    private static String authServerUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> authServerUrl() + "/realms/" + REALM);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> authServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/certs");
        registry.add("iclinic.keycloak.client-id", () -> API_CLIENT);
    }

    @org.springframework.boot.test.web.server.LocalServerPort
    private int puerto;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate rest = new RestTemplate();

    private static final UUID SUB_DENTISTA = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID SUB_RECEPCION = UUID.fromString("00000000-0000-4000-8000-000000000003");

    @BeforeEach
    void proyectarSujetos() {
        proyectar(SUB_DENTISTA, "maria.rodriguez@test.ec", UserRole.DENTIST);
        proyectar(SUB_RECEPCION, "carlos.mendoza@test.ec", UserRole.RECEPTIONIST);
    }

    @Test
    @DisplayName("un token con el rol exigido entra en el endpoint")
    void tokenConRolCorrectoEntra() {
        var respuesta = llamar("/api/v1/auth/me", token("maria.rodriguez@test.ec"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("un token sin el rol exigido recibe 403, no 401")
    void tokenSinElRolRecibe403() {
        // Las conexiones de canal son de administracion; recepcion no las gestiona.
        // (Recepcion SI puede LEER empresas y sucursales, asi que ese endpoint no
        //  serviria para esta prueba.)
        var respuesta = llamar("/api/v1/crm/channels", token("carlos.mendoza@test.ec"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("un token emitido para otro cliente recibe 401 aunque la firma sea valida")
    void tokenConAudienciaAjenaRecibe401() {
        // Mismo realm, misma firma, mismo emisor: solo cambia el `aud`. Sin
        // AudienceValidator este token entraria como si fuera propio.
        var respuesta = llamar("/api/v1/auth/me", tokenDeOtroCliente());
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("un token emitido antes de tokens_valid_from recibe 401")
    void tokenAnteriorALaRevocacionRecibe401() {
        String jwt = token("maria.rodriguez@test.ec");
        assertThat(llamar("/api/v1/auth/me", jwt).getStatusCode()).isEqualTo(HttpStatus.OK);

        // El interruptor de emergencia: sin tocar Keycloak ni esperar a que
        // expire el token, la siguiente peticion con ese mismo token falla.
        var user = userRepository.findByKeycloakUserId(SUB_DENTISTA).orElseThrow();
        user.setTokensValidFrom(Instant.now().plusSeconds(60));
        userRepository.saveAndFlush(user);

        assertThat(llamar("/api/v1/auth/me", jwt).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("una ruta no declarada se deniega en lugar de quedar abierta")
    void rutaNoDeclaradaSeDeniega() {
        var respuesta = llamar("/api/v1/ruta-que-nadie-declaro", token("maria.rodriguez@test.ec"));
        assertThat(respuesta.getStatusCode())
                .as("anyRequest().denyAll(): un endpoint nuevo sin regla no puede nacer abierto")
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    // ---------- utilidades ----------

    private void proyectar(UUID sub, String email, UserRole role) {
        if (userRepository.findByKeycloakUserId(sub).isPresent()) return;
        var user = EcuadorianUser.builder()
                .firstName("Test")
                .lastName(role.name())
                .email(email)
                .role(role)
                .documentType(DocumentType.CEDULA_EC)
                .active(true)
                .keycloakUserId(sub)
                .subjectType(SubjectType.HUMAN)
                .tokensValidFrom(Instant.EPOCH)
                .build();
        userRepository.saveAndFlush(user);
    }

    private ResponseEntity<String> llamar(String ruta, String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        try {
            return rest.exchange("http://localhost:" + puerto + ruta,
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    private String token(String usuario) {
        return solicitarToken("iclinic-web-test", usuario);
    }

    /**
     * Token del mismo realm pero sin {@code iclinic-api} en la audiencia.
     * <p>
     * El usuario tiene que ser uno SIN roles de cliente en {@code iclinic-api}.
     * Keycloak trae de serie el mapper <em>audience resolve</em> en el ambito
     * {@code roles}, que anade a {@code aud} todo cliente en el que el usuario
     * tenga algun rol: con maria, que es DENTIST, hasta el token de
     * {@code otro-cliente-test} saldria con la audiencia correcta y la prueba
     * pasaria sin probar nada.
     */
    private String tokenDeOtroCliente() {
        return solicitarToken("otro-cliente-test", "ajeno@test.ec");
    }

    @SuppressWarnings("unchecked")
    private String solicitarToken(String clientId, String usuario) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", usuario);
        form.add("password", "dev");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> cuerpo = rest.postForObject(
                authServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token",
                new HttpEntity<>(form, headers), Map.class);

        return (String) cuerpo.get("access_token");
    }
}
