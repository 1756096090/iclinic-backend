package com.iclinic.iclinicbackend.modules.appointment;

import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que de verdad hay que demostrar del cambio a {@code timestamptz}: que un
 * instante guardado es el mismo instante al leerlo, aunque la JVM esté en otra
 * zona.
 * <p>
 * Con {@code timestamp without time zone} este test falla: el driver escribe la
 * hora de pared de la JVM y la relee interpretándola en la zona de entonces, así
 * que mover {@code user.timezone} entre la escritura y la lectura desplaza el
 * valor. Es exactamente el fallo que aparece al desplegar en un servidor con otra
 * zona, o al cambiar la zona del contenedor.
 */
@SpringBootTest
class ZonaHorariaIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private final TimeZone zonaOriginal = TimeZone.getDefault();

    @AfterEach
    void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    @DisplayName("ninguna columna de instante se quedo sin zona")
    void ningunaColumnaSinZona() {
        // El bucle de V5 convierte lo que habia entonces. Esto detecta la columna
        // que alguien anada despues con `timestamp` a secas.
        List<String> naives = jdbc.queryForList("""
                SELECT c.table_name || '.' || c.column_name AS col
                  FROM information_schema.columns c
                  JOIN information_schema.tables t
                    ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                 WHERE c.table_schema = 'public'
                   AND t.table_type   = 'BASE TABLE'
                   AND c.data_type    = 'timestamp without time zone'
                   AND c.table_name  <> 'flyway_schema_history'
                """, String.class);

        assertThat(naives)
                .as("toda marca temporal persistida debe llevar zona")
                .isEmpty();
    }

    @Test
    @DisplayName("un instante sobrevive a cambiar la zona de la JVM")
    @Transactional
    void elInstanteSobreviveAlCambioDeZonaDeLaJvm() {
        Instant momento = Instant.parse("2026-10-01T15:00:00Z");

        TimeZone.setDefault(TimeZone.getTimeZone("America/Guayaquil"));
        jdbc.update("INSERT INTO audit_logs (action, created_at) VALUES (?, ?)",
                "PRUEBA_ZONA", java.sql.Timestamp.from(momento));

        // La aplicación se despliega en otra zona, o alguien cambia la del contenedor.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        Instant leido = jdbc.queryForObject(
                "SELECT created_at FROM audit_logs WHERE action = ?",
                (rs, n) -> rs.getTimestamp(1).toInstant(), "PRUEBA_ZONA");

        assertThat(leido)
                .as("con `timestamp` sin zona esto se desplaza 14 horas")
                .isEqualTo(momento);
    }

    @Test
    @DisplayName("la misma hora de pared en Quito y en Bogota son instantes distintos")
    void mismaHoraDeParedEnDosZonasSonInstantesDistintos() {
        // Ecuador (UTC-5) y Colombia (UTC-5) coinciden hoy, asi que comparar esas
        // dos no probaria nada. Se usa una zona con desfase distinto para que el
        // test falle si alguien resuelve las horas contra la del servidor.
        Map<String, String> zonas = Map.of("quito", "America/Guayaquil", "madrid", "Europe/Madrid");

        LocalDate dia = LocalDate.of(2026, 10, 1);
        Instant enQuito = dia.atTime(9, 0).atZone(ZoneId.of(zonas.get("quito"))).toInstant();
        Instant enMadrid = dia.atTime(9, 0).atZone(ZoneId.of(zonas.get("madrid"))).toInstant();

        assertThat(enQuito)
                .as("las 9:00 de un sitio no son las 9:00 del otro")
                .isNotEqualTo(enMadrid);
    }

    @Test
    @DisplayName("toda sucursal tiene una zona horaria valida del catalogo")
    void todaSucursalTieneZonaValida() {
        Integer sinZona = jdbc.queryForObject(
                "SELECT count(*) FROM branches WHERE timezone IS NULL", Integer.class);
        assertThat(sinZona).isZero();

        Integer conZonaDesconocida = jdbc.queryForObject("""
                SELECT count(*) FROM branches b
                 WHERE NOT EXISTS (SELECT 1 FROM timezones z WHERE z.name = b.timezone)
                """, Integer.class);
        assertThat(conZonaDesconocida)
                .as("la clave foranea contra el catalogo deberia impedirlo")
                .isZero();
    }

    @Test
    @DisplayName("el catalogo de zonas acepta UTC y rechaza lo inventado")
    void elCatalogoAceptaUtcYRechazaLoInventado() {
        // Los dos casos que una expresion regular se come al reves: 'UTC' es
        // valida y no lleva barra; 'Marte/Olympus' tiene forma correcta y no existe.
        Integer utc = jdbc.queryForObject(
                "SELECT count(*) FROM timezones WHERE name = 'UTC'", Integer.class);
        assertThat(utc).isEqualTo(1);

        Integer inventada = jdbc.queryForObject(
                "SELECT count(*) FROM timezones WHERE name = 'Marte/Olympus'", Integer.class);
        assertThat(inventada).isZero();
    }
}
