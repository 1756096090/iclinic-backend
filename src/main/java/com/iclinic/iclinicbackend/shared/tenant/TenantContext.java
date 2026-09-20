package com.iclinic.iclinicbackend.shared.tenant;

/**
 * La empresa sobre la que opera la petición en curso.
 * <p>
 * Es un {@link ThreadLocal} y no un bean de ámbito de petición a propósito: lo
 * lee el interceptor de conexiones JDBC, que corre fuera del ciclo de vida de la
 * petición web. Quien lo pone es responsable de limpiarlo en un {@code finally};
 * si no, la siguiente petición que reutilice el hilo del pool hereda el tenant
 * de la anterior, que es la peor fuga posible.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> EMPRESA = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long companyId) {
        EMPRESA.set(companyId);
    }

    /** {@code null} si no hay tenant fijado: la política de RLS no devolverá nada. */
    public static Long get() {
        return EMPRESA.get();
    }

    public static void clear() {
        EMPRESA.remove();
    }

    /**
     * Ejecuta algo con un tenant concreto y lo restaura al terminar. Para hilos
     * que no vienen de una petición: procesos por lotes, planificadores,
     * procesamiento de webhooks.
     */
    public static <T> T runAs(Long companyId, java.util.function.Supplier<T> accion) {
        Long anterior = EMPRESA.get();
        EMPRESA.set(companyId);
        try {
            return accion.get();
        } finally {
            if (anterior == null) {
                EMPRESA.remove();
            } else {
                EMPRESA.set(anterior);
            }
        }
    }
}
