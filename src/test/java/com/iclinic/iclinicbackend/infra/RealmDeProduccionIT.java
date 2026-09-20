package com.iclinic.iclinicbackend.infra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida el realm que se despliega, {@code infra/keycloak/realm-iclinic.json}.
 * <p>
 * No es redundante con {@link com.iclinic.iclinicbackend.modules.auth.KeycloakAuthenticationIT},
 * que usa un realm de pruebas reducido: aquí se comprueba el fichero real, el que
 * monta {@code docker-compose}.
 * <p>
 * Existe porque el importador de Keycloak <strong>no ignora campos
 * desconocidos</strong>: una clave inventada aborta el arranque con
 * {@code Unrecognized field} y el contenedor sale con código 1. Durante el
 * bloque A eso pasó tres veces seguidas —{@code _comentario}, {@code roles} en un
 * cliente, {@code serviceAccountsRealmRoles}—, y cada vez el síntoma fue el
 * mismo: el entorno entero sin levantar. Es un fallo que hay que descubrir en el
 * build, no desplegando.
 */
class RealmDeProduccionIT {

    private static final String REALM = "iclinic";
    private static final Path FICHERO_REALM = Path.of("infra", "keycloak", "realm-iclinic.json");

    @SuppressWarnings("resource")
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
                    .withExposedPorts(8080)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(FICHERO_REALM),
                            "/opt/keycloak/data/import/realm-iclinic.json")
                    .withCommand("start-dev", "--import-realm")
                    .waitingFor(Wait.forHttp("/realms/" + REALM)
                            .forPort(8080)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    private static final RestTemplate REST = new RestTemplate();
    private static String urlBase;
    private static String tokenAdmin;

    @BeforeAll
    static void arrancar() throws Exception {
        assertThat(Files.exists(FICHERO_REALM))
                .as("el fichero de realm versionado debe existir")
                .isTrue();
        KEYCLOAK.start();
        urlBase = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
        tokenAdmin = tokenDeAdmin();
    }

    @Test
    @DisplayName("el realm versionado importa sin campos desconocidos")
    void elRealmImporta() {
        // Que el contenedor haya arrancado ya lo demuestra: la espera es contra
        // /realms/iclinic, que solo responde 200 si el import terminó.
        assertThat(KEYCLOAK.isRunning()).isTrue();
    }

    @Test
    @DisplayName("los client scopes de serie siguen existiendo")
    void losScopesDeSerieExisten() {
        // Declarar `clientScopes` en el import SUSTITUYE a los de Keycloak. Sin el
        // scope `roles` el token no lleva resource_access, el conversor no ve
        // ningún rol y TODO responde 403 con un token perfectamente válido.
        List<String> scopes = admin("/client-scopes").stream()
                .map(s -> (String) s.get("name"))
                .toList();

        assertThat(scopes).contains("roles", "profile", "email", "web-origins", "acr");
    }

    @Test
    @DisplayName("el cliente que emite tokens lleva el mapper de audiencia")
    void elMapperDeAudienciaEstaEnElClienteQueEmite() {
        // En iclinic-api no serviría de nada: es bearer-only y no emite tokens.
        String id = idDeCliente("iclinic-web");
        List<Map<String, Object>> mappers = admin("/clients/" + id + "/protocol-mappers/models");

        assertThat(mappers)
                .as("sin este mapper, AudienceValidator rechaza con 401 todo token de la SPA")
                .anySatisfy(m -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> config = (Map<String, String>) m.get("config");
                    assertThat(config.get("included.client.audience")).isEqualTo("iclinic-api");
                    assertThat(config.get("access.token.claim")).isEqualTo("true");
                });
    }

    @Test
    @DisplayName("el password grant está apagado en los clientes de personas")
    void elPasswordGrantEstaApagado() {
        for (String cliente : List.of("iclinic-web", "iclinic-api")) {
            Map<String, Object> c = admin("/clients?clientId=" + cliente).get(0);
            assertThat((Boolean) c.getOrDefault("directAccessGrantsEnabled", false))
                    .as("%s no debe permitir probar contraseñas sin pasar por el navegador", cliente)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("mfa-required agrupa los roles que exigen OTP")
    void elRolCompuestoDeMfaEstaBienFormado() {
        List<String> contenidos = admin("/roles/mfa-required/composites").stream()
                .map(r -> (String) r.get("name"))
                .toList();

        assertThat(contenidos).containsExactlyInAnyOrder("SUPER_ADMIN", "ADMIN", "DENTIST");
    }

    @Test
    @DisplayName("el flujo de navegador enlazado es el que lleva el OTP condicional")
    void elFlujoDeNavegadorLlevaElOtpCondicional() {
        Map<String, Object> realm = adminObjeto("");
        assertThat(realm.get("browserFlow")).isEqualTo("browser-mfa-condicional");

        List<Map<String, Object>> ejecuciones =
                admin("/authentication/flows/browser-mfa-condicional/executions");

        assertThat(ejecuciones)
                .anySatisfy(e -> assertThat(e.get("providerId")).isEqualTo("conditional-user-role"))
                .anySatisfy(e -> assertThat(e.get("providerId")).isEqualTo("auth-otp-form"));
    }

    @Test
    @DisplayName("los nueve roles de cliente están dados de alta")
    void estanLosNueveRoles() {
        String id = idDeCliente("iclinic-api");
        List<String> roles = admin("/clients/" + id + "/roles").stream()
                .map(r -> (String) r.get("name"))
                .toList();

        assertThat(roles).containsExactlyInAnyOrder(
                "SUPER_ADMIN", "ADMIN", "BRANCH_MANAGER", "DENTIST", "ASSISTANT",
                "RECEPTIONIST", "EXTERNAL_DOCTOR", "BILLING", "PATIENT");
    }

    // ---------- utilidades ----------

    @SuppressWarnings("unchecked")
    private static String tokenDeAdmin() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", "admin");
        form.add("password", "admin");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> cuerpo = REST.postForObject(
                urlBase + "/realms/master/protocol/openid-connect/token",
                new HttpEntity<>(form, headers), Map.class);
        return (String) cuerpo.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> admin(String ruta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAdmin);
        return REST.exchange(urlBase + "/admin/realms/" + REALM + ruta,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), List.class).getBody();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> adminObjeto(String ruta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAdmin);
        return REST.exchange(urlBase + "/admin/realms/" + REALM + ruta,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
    }

    private static String idDeCliente(String clientId) {
        return (String) admin("/clients?clientId=" + clientId).get(0).get("id");
    }
}
