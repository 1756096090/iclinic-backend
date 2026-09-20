package com.iclinic.iclinicbackend.shared.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Fija {@code iclinic.company_id} en cada conexión que entrega el pool, para que
 * la política de RLS tenga contra qué comparar.
 * <p>
 * Va envolviendo al {@code DataSource} y no en un {@code TransactionSynchronization}
 * porque así cubre también lo que no pasa por una transacción de Spring: SQL
 * nativo, informes, {@code JdbcTemplate} suelto. Si el aislamiento depende de que
 * alguien se acordara de abrir una transacción, no es aislamiento.
 * <p>
 * SOBRE EL TERCER PARÁMETRO DE {@code set_config}, que aquí es {@code false} y en
 * la especificación era {@code true}: {@code true} hace la variable local a la
 * transacción, y al adquirir la conexión todavía no hay ninguna abierta, así que
 * se revertiría de inmediato y no serviría de nada. Con {@code false} la variable
 * vive en la sesión, y lo que impide que se filtre entre peticiones es que
 * <strong>se reescribe en cada préstamo del pool</strong>, incluso cuando no hay
 * tenant: en ese caso se pone a vacío, que con la política significa "no ves
 * nada". Dejar el valor anterior sería justo la fuga que esto existe para evitar.
 * <p>
 * La alternativa de la especificación —{@code TransactionSynchronization} con
 * {@code true}— es igual de correcta pero cubre menos: solo lo que pasa por una
 * transacción de Spring, y deja fuera el SQL nativo y los informes.
 */
@Slf4j
public class TenantAwareDataSource extends DelegatingDataSource {

    /**
     * Rol al que cambia cada conexión, o vacío para no cambiar.
     * <p>
     * Es lo que impide que en desarrollo —donde se conecta como {@code postgres},
     * superusuario— RLS quede sin efecto. Un superusuario evita todas las
     * políticas sin error y sin aviso: el aislamiento parecería activo y no lo
     * estaría. Con {@code SET ROLE iclinic_app} la conexión queda sujeta aunque
     * el usuario que abrió la sesión no lo estuviera.
     * <p>
     * Se deja vacío en el perfil de integración: allí los tests hacen su propio
     * {@code SET ROLE} para poder comparar el comportamiento con y sin rol, y la
     * carga del seed necesita saltarse la política.
     */
    private final String rolDeAplicacion;

    public TenantAwareDataSource(DataSource delegate, String rolDeAplicacion) {
        super(delegate);
        this.rolDeAplicacion = rolDeAplicacion == null ? "" : rolDeAplicacion.trim();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return aplicarTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return aplicarTenant(super.getConnection(username, password));
    }

    private Connection aplicarTenant(Connection conexion) throws SQLException {
        Long empresa = TenantContext.get();

        if (!rolDeAplicacion.isEmpty()) {
            try (java.sql.Statement st = conexion.createStatement()) {
                // El nombre viene de configuracion, no de una peticion; aun asi se
                // valida para que nadie pueda colar SQL por una propiedad.
                if (!rolDeAplicacion.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    throw new IllegalStateException("Nombre de rol no valido: " + rolDeAplicacion);
                }
                st.execute("SET ROLE " + rolDeAplicacion);
            }
        }

        // Siempre se escribe, incluso cuando no hay tenant: hay que BORRAR el de
        // la conexión anterior. Dejarlo puesto es exactamente la fuga entre
        // clínicas que esto existe para impedir.
        try (PreparedStatement ps = conexion.prepareStatement("SELECT set_config('iclinic.company_id', ?, false)")) {
            ps.setString(1, empresa == null ? "" : empresa.toString());
            ps.execute();
        }
        return conexion;
    }
}
