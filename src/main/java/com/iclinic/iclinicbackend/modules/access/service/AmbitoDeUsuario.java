package com.iclinic.iclinicbackend.modules.access.service;

import com.iclinic.iclinicbackend.modules.access.entity.CompanyMembership;
import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * El único sitio que responde qué puede ejercer un usuario y dónde.
 * <p>
 * Sustituye a leer {@code users.role}, {@code users.company_id} y
 * {@code users.branch_id} directamente. Esas tres columnas desaparecen, y hasta
 * hoy cada sitio las consultaba por su cuenta: 19 comparaciones de rol y 20 de
 * empresa o sucursal repartidas por seis ficheros.
 * <p>
 * <b>Dos conceptos que el código anterior mezclaba.</b> {@code SUPER_ADMIN} se
 * usaba como si fuera un rol de empresa, y no lo es: un administrador de
 * plataforma <em>no pertenece a ninguna clínica</em> y por tanto no tiene
 * membresía (ver V10). Aquí van separados:
 * <ul>
 *   <li>{@link #esAdminDePlataforma(Long)} — propiedad del usuario, sin empresa.</li>
 *   <li>{@link #tieneRol(Long, Long, UserRole)} — rol <em>dentro de</em> una empresa.</li>
 * </ul>
 * Confundirlos es lo que hacía que un administrador de plataforma necesitara un
 * {@code company_id} que no le corresponde.
 * <p>
 * <b>Por qué la firma lleva {@code userId} y no asume "el actual".</b> Hay sitios
 * que preguntan por el rol de <em>otro</em> usuario —si un doctor es realmente
 * {@code DENTIST}, por ejemplo—. Si esto solo sirviera al usuario en curso, esos
 * sitios la esquivarían y dejaría de ser el único lugar donde vive la respuesta.
 * Las variantes «del usuario actual» viven en {@code CurrentUserService} y se
 * apoyan en ésta, no al revés.
 * <p>
 * <b>Memoización por petición, y solo por petición.</b> Pasar de leer un campo a
 * consultar la base con 19 llamadas, alguna en bucle, provocaría un N+1. Las
 * membresías se cargan una vez por petición. No se cachea más allá: revocar un
 * rol tiene que surtir efecto en la llamada siguiente, no cuando caduque una
 * caché. Es el mismo motivo por el que {@code TenantContext} tampoco se cachea.
 */
@Service
@RequiredArgsConstructor
public class AmbitoDeUsuario {

    private static final String CLAVE_MEMORIA = AmbitoDeUsuario.class.getName() + ".membresias.";

    private final CompanyMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    // ───────────────────────── Rol dentro de una empresa ─────────────────────

    @Transactional(readOnly = true)
    public boolean tieneRol(Long userId, Long companyId, UserRole rol) {
        if (userId == null || companyId == null || rol == null) return false;
        return membresias(userId).stream()
                .filter(m -> m.getCompany().getId().equals(companyId))
                .anyMatch(m -> m.getRole() == rol);
    }

    @Transactional(readOnly = true)
    public boolean tieneAlgunRol(Long userId, Long companyId, UserRole... roles) {
        return Arrays.stream(roles).anyMatch(r -> tieneRol(userId, companyId, r));
    }

    /** {@code true} si tiene ese rol en <em>cualquiera</em> de sus empresas. */
    @Transactional(readOnly = true)
    public boolean tieneRolEnAlgunaEmpresa(Long userId, UserRole rol) {
        if (userId == null || rol == null) return false;
        return membresias(userId).stream().anyMatch(m -> m.getRole() == rol);
    }

    // ───────────────────────── Administrador de plataforma ───────────────────

    /**
     * Opera por encima de las clínicas y no pertenece a ninguna.
     * <p>
     * En el bloque D esto deja de ser una columna booleana y pasa a ser un rol de
     * realm en Keycloak con MFA obligatorio: un booleano en una fila que salta
     * todo el modelo de permisos no es un permiso, es una puerta trasera.
     */
    @Transactional(readOnly = true)
    public boolean esAdminDePlataforma(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsPlatformAdmin()))
                .orElse(false);
    }

    // ───────────────────────── Ámbito: empresa y sucursal ────────────────────

    @Transactional(readOnly = true)
    public Set<Long> empresasDe(Long userId) {
        return membresias(userId).stream()
                .map(m -> m.getCompany().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public boolean perteneceALaEmpresa(Long userId, Long companyId) {
        return companyId != null && empresasDe(userId).contains(companyId);
    }

    /**
     * Sucursales a las que la membresía da acceso.
     * <p>
     * Una membresía <em>sin</em> sucursales significa acceso a todas las de la
     * empresa, no a ninguna. Es la semántica que ya tenía {@code membership_branches}
     * y conviene no invertirla al migrar: hacerlo dejaría fuera a todo el mundo.
     */
    @Transactional(readOnly = true)
    public boolean tieneAccesoALaSucursal(Long userId, Long branchId) {
        if (branchId == null) return false;
        return membresias(userId).stream().anyMatch(m -> {
            Set<Branch> sucursales = m.getBranches();
            return sucursales == null || sucursales.isEmpty()
                    || sucursales.stream().anyMatch(b -> b.getId().equals(branchId));
        });
    }

    /**
     * La empresa sobre la que opera cuando no se indica otra.
     * <p>
     * Vacío para un administrador de plataforma. Con varias membresías devuelve la
     * primera: el multi-empresa de verdad llega en el bloque D con la cabecera
     * {@code X-Organization-Id}, y hasta entonces elegir aquí sería inventarse una
     * regla que luego habría que deshacer.
     */
    @Transactional(readOnly = true)
    public Optional<Long> empresaPrincipalDe(Long userId) {
        return membresias(userId).stream().map(m -> m.getCompany().getId()).findFirst();
    }

    // ───────────────────────── Memoización ───────────────────────────────────

    private List<CompanyMembership> membresias(Long userId) {
        if (userId == null) return List.of();

        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        if (peticion == null) {
            // Fuera de una petición —trabajos por lotes, tests, webhooks— no hay
            // dónde memorizar y tampoco hace falta: no es el camino caliente.
            return cargar(userId);
        }

        String clave = CLAVE_MEMORIA + userId;
        @SuppressWarnings("unchecked")
        List<CompanyMembership> memorizadas =
                (List<CompanyMembership>) peticion.getAttribute(clave, RequestAttributes.SCOPE_REQUEST);
        if (memorizadas != null) {
            return memorizadas;
        }

        List<CompanyMembership> cargadas = cargar(userId);
        peticion.setAttribute(clave, cargadas, RequestAttributes.SCOPE_REQUEST);
        return cargadas;
    }

    private List<CompanyMembership> cargar(Long userId) {
        return membershipRepository.findActivasConEmpresaYSucursales(userId);
    }
}
