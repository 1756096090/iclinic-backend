package com.iclinic.iclinicbackend.shared.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Avisa al arrancar si la conexión de la aplicación <strong>evita</strong> el
 * aislamiento por tenant.
 * <p>
 * Existe porque el fallo es invisible. Un superusuario de PostgreSQL —y un
 * usuario con {@code BYPASSRLS}— se salta todas las políticas de RLS sin error,
 * sin aviso y sin que ninguna consulta se comporte de forma rara: simplemente
 * devuelve también las filas de las otras clínicas. En desarrollo se conecta como
 * {@code postgres}, así que ahí el aislamiento <em>no protege nada</em> y todo
 * parece funcionar. Si ese mismo descuido llega a producción, la primera señal
 * sería un cliente viendo pacientes ajenos.
 * <p>
 * La comprobación cuesta una consulta al arrancar y convierte un fallo silencioso
 * en un mensaje que no se puede pasar por alto.
 */
@Component
@Slf4j
public class ComprobacionDeAislamiento {

    /** Perfiles donde esto es un aviso y no un fallo. Cualquier otro, aborta. */
    private static final List<String> PERFILES_DE_DESARROLLO = List.of("dev", "it", "local");

    private final JdbcTemplate jdbc;
    private final Environment entorno;

    public ComprobacionDeAislamiento(JdbcTemplate jdbc, Environment entorno) {
        this.jdbc = jdbc;
        this.entorno = entorno;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void comprobar() {
        Boolean evitaRls = jdbc.queryForObject("""
                SELECT rolsuper OR rolbypassrls
                  FROM pg_roles WHERE rolname = current_user
                """, Boolean.class);

        String usuario = jdbc.queryForObject("SELECT current_user", String.class);

        if (!Boolean.TRUE.equals(evitaRls)) {
            log.info("Aislamiento por tenant activo: la conexion '{}' esta sujeta a RLS", usuario);
            return;
        }

        boolean esDesarrollo = Arrays.stream(entorno.getActiveProfiles())
                .anyMatch(PERFILES_DE_DESARROLLO::contains);

        if (!esDesarrollo) {
            // Un aviso se ignora a la tercera semana. Fuera de desarrollo esto es
            // una condicion de seguridad: la aplicacion no debe levantar viendo
            // los datos de todas las clinicas.
            throw new IllegalStateException(
                    "La aplicacion conecta como '" + usuario + "', que evita RLS (superusuario o "
                    + "BYPASSRLS). El aislamiento entre clinicas NO estaria activo. Conecta como "
                    + "iclinic_app. Perfiles activos: "
                    + String.join(",", entorno.getActiveProfiles()));
        }

        {
            log.warn("""

                    ===========================================================
                     EL AISLAMIENTO POR TENANT NO ESTA ACTIVO
                     La aplicacion conecta como '{}', que es superusuario o
                     tiene BYPASSRLS. Las politicas de RLS NO se le aplican:
                     una consulta sin filtrar devuelve datos de TODAS las
                     clinicas, sin error y sin aviso.
                     En cualquier despliegue real hay que conectar como
                     iclinic_app. Ver V29__rls.sql.
                    ===========================================================""", usuario);
        }
    }
}
