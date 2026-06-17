package com.iclinic.iclinicbackend.modules.access.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ejecuta el backfill de membresías al arrancar la aplicación.
 *
 * <p>Controlado por la propiedad {@code iclinic.multitenancy.backfill-on-startup}
 * (por defecto {@code true}). Al ser idempotente, es seguro dejarlo activo; desactivarlo
 * una vez completada la Fase D.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "iclinic.multitenancy",
        name = "backfill-on-startup",
        havingValue = "true",
        matchIfMissing = true
)
public class MembershipBackfillRunner implements ApplicationRunner {

    private final MembershipBackfillService backfillService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillService.backfill();
        } catch (Exception e) {
            // No bloquear el arranque si el backfill falla; sólo registrar.
            log.error("Membership backfill failed on startup", e);
        }
    }
}
