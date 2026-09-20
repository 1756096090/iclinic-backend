package com.iclinic.iclinicbackend.shared.tenant;

import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lo que este bloque tiene que demostrar: que el aislamiento entre clínicas vive
 * en el motor y no en el código.
 * <p>
 * Todas las comprobaciones se hacen conectando como {@code iclinic_app}, que es
 * el rol de la aplicación: sin {@code BYPASSRLS} y sin ser propietario de las
 * tablas. Hacerlas como {@code postgres} no probaría nada, porque un
 * superusuario siempre evita RLS — y ése es justo el motivo de que este fallo no
 * aparezca en desarrollo y sí en producción.
 */
@ActiveProfiles("it")
@SpringBootTest
class AislamientoRlsIT extends AbstractPostgresIT {

    private static final long EMPRESA_1 = 1L;
    private static final long EMPRESA_2 = 2L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private static Long citaDeLaEmpresa2;

    @BeforeAll
    static void prepararRolDeAplicacion() {
        // Nada: el rol lo crea V29. El seed trae las dos empresas.
    }

    // ---------- el criterio que más importa ----------

    @Test
    @DisplayName("una cita de OTRA empresa no existe, ni buscandola por su id")
    void unaCitaDeOtraEmpresaNoExistePorId() throws SQLException {
        long id = citaAjena();

        // Como empresa 1, pidiendo explicitamente el id de una cita de la 2.
        // Esto es el IDOR: el atacante ya sabe el id. Aqui muere en el motor.
        List<Long> encontradas = comoApp(EMPRESA_1,
                "SELECT id FROM appointments WHERE id = " + id);

        assertThat(encontradas)
                .as("el IDOR entre clinicas tiene que morir en PostgreSQL, no en el servicio")
                .isEmpty();
    }

    @Test
    @DisplayName("sin tenant fijado no se ve ninguna fila: falla cerrada")
    void sinTenantNoSeVeNada() throws SQLException {
        List<Long> todas = comoApp(null, "SELECT id FROM appointments");
        assertThat(todas)
                .as("sin iclinic.company_id la politica es NULL y no devuelve nada")
                .isEmpty();
    }

    @Test
    @DisplayName("cada empresa ve las suyas y solo las suyas")
    void cadaEmpresaVeSoloLoSuyo() throws SQLException {
        List<Long> deLa1 = comoApp(EMPRESA_1, "SELECT company_id FROM crm_contacts");
        List<Long> deLa2 = comoApp(EMPRESA_2, "SELECT company_id FROM crm_contacts");

        assertThat(deLa1).isNotEmpty().allMatch(c -> c == EMPRESA_1);
        assertThat(deLa2).allMatch(c -> c == EMPRESA_2);
    }

    @Test
    @DisplayName("insertar con el company_id de otra empresa lo rechaza la politica")
    void insertarEnOtraEmpresaSeRechaza() {
        assertThatThrownBy(() -> ejecutarComoApp(EMPRESA_1, """
                INSERT INTO crm_contacts (company_id, full_name, source_channel, active, created_at, updated_at)
                VALUES (2, 'Infiltrado', 'WHATSAPP', true, now(), now())
                """))
                .hasMessageContaining("row-level security");
    }

    // ---------- el rol, que es lo que hace que esto no sea decorativo ----------

    @Test
    @DisplayName("iclinic_app no tiene BYPASSRLS ni es propietario de las tablas")
    void elRolDeLaAplicacionEstaBienAcotado() {
        Boolean bypass = jdbc.queryForObject(
                "SELECT rolbypassrls FROM pg_roles WHERE rolname = 'iclinic_app'", Boolean.class);
        assertThat(bypass)
                .as("con BYPASSRLS todo lo demas de este fichero seria teatro")
                .isFalse();

        Boolean superusuario = jdbc.queryForObject(
                "SELECT rolsuper FROM pg_roles WHERE rolname = 'iclinic_app'", Boolean.class);
        assertThat(superusuario).isFalse();

        Integer propias = jdbc.queryForObject("""
                SELECT count(*) FROM pg_class c
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                  JOIN pg_roles r ON r.oid = c.relowner
                 WHERE n.nspname = 'public' AND c.relkind = 'r' AND r.rolname = 'iclinic_app'
                """, Integer.class);
        assertThat(propias)
                .as("el propietario se salta la politica si no esta FORCE, y aun asi mejor que no lo sea")
                .isZero();
    }

    @Test
    @DisplayName("la aplicacion no puede borrar: las bajas son logicas")
    void laAplicacionNoPuedeBorrar() {
        Integer conDelete = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.role_table_grants
                 WHERE grantee = 'iclinic_app' AND privilege_type = 'DELETE'
                """, Integer.class);
        assertThat(conDelete).isZero();
    }

    // ---------- cobertura: la que detecta la tabla nueva sin politica ----------

    @Test
    @DisplayName("toda tabla con company_id tiene RLS activo, forzado y con politica")
    void ningunaTablaTenantScopedSeQuedoSinPolitica() {
        List<String> desprotegidas = jdbc.queryForList("""
                SELECT c.relname
                  FROM pg_class c
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                  JOIN pg_attribute a ON a.attrelid = c.oid AND a.attname = 'company_id'
                                     AND a.attnum > 0 AND NOT a.attisdropped
                 WHERE n.nspname = 'public'
                   AND c.relkind IN ('r','p')
                   AND c.relname <> 'users'
                   AND (NOT c.relrowsecurity
                     OR NOT c.relforcerowsecurity
                     OR NOT EXISTS (SELECT 1 FROM pg_policy p WHERE p.polrelid = c.oid))
                """, String.class);

        assertThat(desprotegidas)
                .as("una tabla con company_id sin politica tiene CERO aislamiento, y en silencio")
                .isEmpty();
    }

    // ---------- el pool ----------

    @Test
    @DisplayName("el DataSource reescribe el tenant en cada prestamo del pool")
    void elPoolNoArrastraElTenantDeLaPeticionAnterior() {
        // Este test mira la VARIABLE, no las filas, y es deliberado: la conexion
        // de los tests es superusuario del contenedor, asi que evita RLS y
        // contar filas aqui no probaria nada. Lo que le toca demostrar al
        // DataSource envuelto es que reescribe iclinic.company_id en cada
        // prestamo; que la politica lo aplique ya lo prueban los casos de arriba,
        // que conectan explicitamente como iclinic_app.
        TenantContext.set(EMPRESA_1);
        String primera;
        try {
            primera = jdbc.queryForObject(
                    "SELECT current_setting('iclinic.company_id', true)", String.class);
        } finally {
            TenantContext.clear();
        }

        TenantContext.set(EMPRESA_2);
        String segunda;
        try {
            segunda = jdbc.queryForObject(
                    "SELECT current_setting('iclinic.company_id', true)", String.class);
        } finally {
            TenantContext.clear();
        }

        String sinTenant = jdbc.queryForObject(
                "SELECT current_setting('iclinic.company_id', true)", String.class);

        assertThat(primera).isEqualTo("1");
        assertThat(segunda)
                .as("la segunda peticion no puede heredar el tenant de la primera")
                .isEqualTo("2");
        assertThat(sinTenant)
                .as("al soltar el tenant la variable queda vacia, y con ella la politica no ve nada")
                .isEmpty();
    }

    @Test
    @DisplayName("la conexion de los tests evita RLS, y por eso el aislamiento se prueba con SET ROLE")
    void laConexionDeLosTestsEsSuperusuario() {
        // No es un defecto: es la razon de que los demas tests hagan SET ROLE.
        // Se deja escrito para que nadie "simplifique" quitandolo y crea que
        // sigue probando aislamiento.
        Boolean evita = jdbc.queryForObject(
                "SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = current_user",
                Boolean.class);
        assertThat(evita)
                .as("si esto dejara de ser cierto, los SET ROLE de este fichero sobrarian")
                .isTrue();
    }

    // ---------- utilidades ----------

    /** Conexión como {@code iclinic_app}, que es quien de verdad está sujeto a RLS. */
    private List<Long> comoApp(Long empresa, String sql) throws SQLException {
        List<Long> salida = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("SET ROLE iclinic_app");
            st.execute("SELECT set_config('iclinic.company_id', '"
                    + (empresa == null ? "" : empresa) + "', false)");
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    salida.add(rs.getLong(1));
                }
            }
            st.execute("RESET ROLE");
        }
        return salida;
    }

    private void ejecutarComoApp(Long empresa, String sql) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("SET ROLE iclinic_app");
            st.execute("SELECT set_config('iclinic.company_id', '" + empresa + "', false)");
            try {
                st.execute(sql);
            } finally {
                st.execute("RESET ROLE");
            }
        }
    }

    /** Una cita de la empresa 2, creada saltándose RLS (como haría una migración). */
    private long citaAjena() {
        if (citaDeLaEmpresa2 != null) return citaDeLaEmpresa2;
        citaDeLaEmpresa2 = jdbc.queryForObject("""
                INSERT INTO appointments (company_id, branch_id, contact_id, doctor_id,
                                          scheduled_start, scheduled_end, status, created_at, updated_at)
                SELECT 2, b.id, c.id, u.id,
                       now() + interval '1 day', now() + interval '1 day 30 minutes',
                       'SCHEDULED', now(), now()
                  FROM branches b, crm_contacts c, users u
                 WHERE b.company_id = 2 AND c.company_id = 2 AND u.id = 4
                 LIMIT 1
                RETURNING id
                """, Long.class);
        return citaDeLaEmpresa2;
    }
}
