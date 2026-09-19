package com.iclinic.iclinicbackend.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests de integración que necesitan PostgreSQL de verdad.
 * <p>
 * H2 no sirve para lo que viene: no soporta {@code EXCLUDE}, ni tipos rango, ni
 * RLS, ni índices parciales. Una prueba en H2 de cualquiera de esas cosas no
 * prueba nada, y lo peor es que pasa en verde.
 * <p>
 * El contenedor es {@code static} y no se declara con {@code @Container}: así no
 * lo para JUnit entre clases y las suites sucesivas lo reutilizan. Lo cierra la
 * JVM al terminar, vía el <em>ryuk</em> de Testcontainers.
 */
@Testcontainers
public abstract class AbstractPostgresIT {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("iclinic_test")
                    .withUsername("iclinic")
                    .withPassword("iclinic")
                    .withReuse(true);

    static {
        POSTGRES.start();
    }
}
