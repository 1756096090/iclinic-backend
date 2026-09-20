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

    public TenantAwareDataSource(DataSource delegate) {
        super(delegate);
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
