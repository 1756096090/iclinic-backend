package com.iclinic.iclinicbackend.shared.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
@Slf4j
public class ComprobacionDeAislamiento {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void comprobar() {
        Boolean evitaRls = jdbc.queryForObject("""
                SELECT rolsuper OR rolbypassrls
                  FROM pg_roles WHERE rolname = current_user
                """, Boolean.class);

        String usuario = jdbc.queryForObject("SELECT current_user", String.class);

        if (Boolean.TRUE.equals(evitaRls)) {
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
        } else {
            log.info("Aislamiento por tenant activo: la conexion '{}' esta sujeta a RLS", usuario);
        }
    }
}
