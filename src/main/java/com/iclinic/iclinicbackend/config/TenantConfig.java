package com.iclinic.iclinicbackend.config;

import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.shared.tenant.TenantAwareDataSource;
import com.iclinic.iclinicbackend.shared.tenant.TenantContextFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Cableado del aislamiento por tenant.
 */
@Configuration
public class TenantConfig {

    /**
     * Envuelve el {@code DataSource} que haya creado Spring Boot, en lugar de
     * declarar uno propio.
     * <p>
     * Declararlo aquí parecía más claro y estaba mal: sustituir el bean se salta
     * la autoconfiguración, y con ella el {@code @ServiceConnection} de
     * Testcontainers, que es como los tests de integración inyectan la URL del
     * contenedor. El síntoma era un {@code Failed to determine a suitable driver
     * class} en todos ellos. Envolviendo, la aplicación y los tests conservan su
     * propia forma de configurar la conexión y ambos quedan cubiertos.
     * <p>
     * Al ser el único {@code DataSource} del contexto, no hay manera de pedir una
     * conexión "por otro lado" y saltarse el tenant.
     */
    @Bean
    public static BeanPostProcessor envolverDataSourceConTenant(
            org.springframework.core.env.Environment entorno) {
        String rol = entorno.getProperty("iclinic.tenant.db-role", "");
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) throws BeansException {
                if (bean instanceof DataSource ds && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(ds, rol);
                }
                return bean;
            }
        };
    }

    @Bean
    public TenantContextFilter tenantContextFilter(
            CompanyMembershipRepository membershipRepository,
            com.iclinic.iclinicbackend.modules.user.repository.UserRepository userRepository) {
        return new TenantContextFilter(membershipRepository, userRepository);
    }

    /**
     * Impide el registro automático del filtro en la cadena de servlets global.
     * <p>
     * Todo bean de tipo {@code Filter} se auto-registra en el contenedor, y sin
     * orden explícito queda por detrás del {@code FilterChainProxy}: se ejecutaría
     * dos veces, y la registración que mandaría sería la accidental. Es el mismo
     * problema que tuvo {@code TokenRevocationFilter} en el bloque A, donde además
     * ocultó que el filtro estaba mal colocado.
     */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterSinRegistroGlobal(
            TenantContextFilter filtro) {
        FilterRegistrationBean<TenantContextFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }
}
