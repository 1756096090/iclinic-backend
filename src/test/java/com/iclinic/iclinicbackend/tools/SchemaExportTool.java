package com.iclinic.iclinicbackend.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Utilidad de desarrollo: vuelca el DDL PostgreSQL de las entidades JPA a
 * build/generated-schema.sql para construir la migración baseline de Flyway.
 * <p>
 * Usa la generación de esquema estándar de JPA con el dialecto de PostgreSQL
 * forzado, de modo que el script sale con las mismas naming strategies que
 * aplica Spring Boot en runtime. La conexión H2 no se usa para DDL
 * ({@code ddl-auto=none}); sólo existe para arrancar el EntityManagerFactory.
 * <p>
 * No es un test de la aplicación: no valida comportamiento.
 */
@SpringBootTest
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:schemagen;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.properties.hibernate.type.preferred_enum_jdbc_type=VARCHAR",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/generated-schema.sql",
        "spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata",
        "telegram.webhook.auto-register-enabled=false",
        "telegram.webhook.cloudflare.enabled=false"
})
class SchemaExportTool {

    @Test
    void exportPostgresSchema() {
        // El script se escribe al construir el EntityManagerFactory.
    }
}
