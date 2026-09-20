package com.iclinic.iclinicbackend.modules.access;

import com.iclinic.iclinicbackend.modules.access.service.AmbitoDeUsuario;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El sitio único donde se responde qué puede ejercer alguien y dónde.
 * <p>
 * Los identificadores vienen del seed: usuario 1 ADMIN de la empresa 1, 2 DENTIST
 * de la 1, 4 ADMIN de la 2, y 5 administrador de plataforma sin empresa.
 */
@ActiveProfiles("it")
@SpringBootTest
class AmbitoDeUsuarioIT extends AbstractPostgresIT {

    private static final long ADMIN_EMPRESA_1 = 1L;
    private static final long DENTISTA_EMPRESA_1 = 2L;
    private static final long ADMIN_EMPRESA_2 = 4L;
    private static final long ADMIN_DE_PLATAFORMA = 5L;

    @Autowired
    private AmbitoDeUsuario ambito;

    @Test
    @DisplayName("el rol se responde por empresa, no en abstracto")
    void elRolEsPorEmpresa() {
        assertThat(ambito.tieneRol(ADMIN_EMPRESA_1, 1L, UserRole.ADMIN)).isTrue();

        assertThat(ambito.tieneRol(ADMIN_EMPRESA_1, 2L, UserRole.ADMIN))
                .as("es ADMIN de la empresa 1, no de la 2: preguntar sin empresa seria la fuga")
                .isFalse();

        assertThat(ambito.tieneRol(DENTISTA_EMPRESA_1, 1L, UserRole.ADMIN)).isFalse();
        assertThat(ambito.tieneRol(DENTISTA_EMPRESA_1, 1L, UserRole.DENTIST)).isTrue();
    }

    @Test
    @DisplayName("se puede preguntar por OTRO usuario, no solo por el actual")
    void sePuedePreguntarPorOtroUsuario() {
        // Es el caso de AppointmentServiceImpl, que comprueba si el doctor de una
        // cita es realmente DENTIST. Sin esta firma, ese sitio esquivaria la
        // abstraccion y dejaria de haber un unico lugar con la respuesta.
        assertThat(ambito.tieneRol(DENTISTA_EMPRESA_1, 1L, UserRole.DENTIST)).isTrue();
        assertThat(ambito.tieneRol(ADMIN_EMPRESA_2, 2L, UserRole.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("administrador de plataforma: sin empresa y sin membresia")
    void adminDePlataformaNoPerteneceANingunaEmpresa() {
        assertThat(ambito.esAdminDePlataforma(ADMIN_DE_PLATAFORMA)).isTrue();
        assertThat(ambito.empresasDe(ADMIN_DE_PLATAFORMA))
                .as("no pertenece a ninguna clinica: es lo que V10 dejo claro")
                .isEmpty();
        assertThat(ambito.empresaPrincipalDe(ADMIN_DE_PLATAFORMA)).isEmpty();

        assertThat(ambito.esAdminDePlataforma(ADMIN_EMPRESA_1))
                .as("ser ADMIN de una empresa no es ser admin de plataforma")
                .isFalse();
    }

    @Test
    @DisplayName("una membresia sin sucursales da acceso a TODAS las de su empresa")
    void membresiaSinSucursalesDaAccesoATodas() {
        // Es la semantica que ya tenia membership_branches. Invertirla al migrar
        // dejaria sin acceso a todo el personal del seed, que no tiene sucursales
        // asignadas explicitamente.
        assertThat(ambito.tieneAccesoALaSucursal(ADMIN_EMPRESA_1, 1L)).isTrue();
        assertThat(ambito.tieneAccesoALaSucursal(ADMIN_EMPRESA_1, 2L)).isTrue();
    }

    @Test
    @DisplayName("pertenecer a una empresa no es pertenecer a otra")
    void laPertenenciaNoSeContagia() {
        assertThat(ambito.perteneceALaEmpresa(ADMIN_EMPRESA_1, 1L)).isTrue();
        assertThat(ambito.perteneceALaEmpresa(ADMIN_EMPRESA_1, 2L)).isFalse();
        assertThat(ambito.perteneceALaEmpresa(ADMIN_EMPRESA_2, 2L)).isTrue();
        assertThat(ambito.perteneceALaEmpresa(ADMIN_EMPRESA_2, 1L)).isFalse();
    }

    @Test
    @DisplayName("los nulos no conceden nada")
    void losNulosNoConcedenNada() {
        // Una abstraccion de autorizacion que devuelve true ante un nulo es peor
        // que no tenerla.
        assertThat(ambito.tieneRol(null, 1L, UserRole.ADMIN)).isFalse();
        assertThat(ambito.tieneRol(ADMIN_EMPRESA_1, null, UserRole.ADMIN)).isFalse();
        assertThat(ambito.tieneRol(ADMIN_EMPRESA_1, 1L, null)).isFalse();
        assertThat(ambito.esAdminDePlataforma(null)).isFalse();
        assertThat(ambito.perteneceALaEmpresa(ADMIN_EMPRESA_1, null)).isFalse();
        assertThat(ambito.tieneAccesoALaSucursal(ADMIN_EMPRESA_1, null)).isFalse();
    }

    @Test
    @DisplayName("un usuario que no existe no tiene nada")
    void usuarioInexistente() {
        long noExiste = 999_999L;
        assertThat(ambito.esAdminDePlataforma(noExiste)).isFalse();
        assertThat(ambito.empresasDe(noExiste)).isEmpty();
        assertThat(ambito.tieneRol(noExiste, 1L, UserRole.ADMIN)).isFalse();
    }
}
