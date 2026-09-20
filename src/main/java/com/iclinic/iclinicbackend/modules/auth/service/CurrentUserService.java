package com.iclinic.iclinicbackend.modules.auth.service;

import com.iclinic.iclinicbackend.modules.access.service.AmbitoDeUsuario;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Las mismas preguntas de antes, pero referidas al usuario autenticado y
 * respondidas por {@link AmbitoDeUsuario}.
 * <p>
 * Hasta ahora esta clase leía {@code user.getRole()}, {@code user.getCompany()} y
 * {@code user.getBranch()} directamente. Esas tres columnas desaparecen: el rol
 * vive en {@code company_memberships.role} y el ámbito en la propia membresía.
 * Aquí no se decide nada nuevo, solo se traduce «el usuario actual» a la firma
 * con {@code userId} y {@code companyId}.
 * <p>
 * <b>Cambio de semántica que conviene conocer.</b> {@code SUPER_ADMIN} dejaba de
 * ser un rol de empresa: quien manda ahora es {@code is_platform_admin}. Los
 * métodos que antes preguntaban {@code role == SUPER_ADMIN} preguntan por el
 * administrador de plataforma, que es lo que de verdad querían decir. Mientras
 * {@code users.role} exista, ambos coinciden —V12 los sincronizó y hay un test
 * que vigila que no diverjan—, así que el comportamiento observable no cambia.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final AmbitoDeUsuario ambito;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (auth.getDetails() instanceof User user) {
            return user;
        }
        // Respaldo: TokenRevocationFilter deja el usuario en los detalles, asi que
        // esta rama solo se alcanza si alguien construye la autenticacion a mano.
        try {
            UUID subject = UUID.fromString(auth.getName());
            return userRepository.findByKeycloakUserId(subject)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sujeto no valido");
        }
    }

    private Long idActual() {
        return getCurrentUser().getId();
    }

    // ───────────────────────── Rol ───────────────────────────────────────────

    /**
     * Administrador de plataforma. Conserva el nombre {@code isSuperAdmin} porque
     * lo llaman doce sitios y renombrarlo aquí solo añadiría ruido al diff; el
     * nombre se corrige en el bloque D, cuando el concepto pase a ser un rol de
     * realm.
     */
    public boolean isSuperAdmin() {
        return ambito.esAdminDePlataforma(idActual());
    }

    public boolean isCompanyAdmin() {
        Long id = idActual();
        return ambito.esAdminDePlataforma(id)
                || ambito.tieneRolEnAlgunaEmpresa(id, UserRole.ADMIN);
    }

    /** Variante con {@code userId} explícito, para preguntar por otro usuario. */
    public boolean tieneRol(Long userId, Long companyId, UserRole rol) {
        return ambito.tieneRol(userId, companyId, rol);
    }

    public void assertSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo SUPER_ADMIN puede ejecutar esta acción");
        }
    }

    // ───────────────────────── Ámbito ────────────────────────────────────────

    public void assertCanAccessCompany(Long companyId) {
        if (!canAccessCompany(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta empresa");
        }
    }

    public boolean canAccessCompany(Long companyId) {
        Long id = idActual();
        if (ambito.esAdminDePlataforma(id)) return true;
        return ambito.perteneceALaEmpresa(id, companyId);
    }

    public void assertCanAccessBranch(Long branchId) {
        if (!canAccessBranch(branchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta sucursal");
        }
    }

    public boolean canAccessBranch(Long branchId) {
        Long id = idActual();
        if (ambito.esAdminDePlataforma(id)) return true;
        return ambito.tieneAccesoALaSucursal(id, branchId);
    }

    /**
     * Empresa del usuario autenticado, o {@code null} si es administrador de
     * plataforma y no opera sobre ninguna en concreto.
     */
    public Long getCurrentCompanyId() {
        return ambito.empresaPrincipalDe(idActual()).orElse(null);
    }

    // ───────────────────────── Gestión de usuarios ───────────────────────────

    public void assertCanManageUser(User targetUser) {
        Long id = idActual();
        if (ambito.esAdminDePlataforma(id)) return;

        Long empresa = ambito.empresaPrincipalDe(id).orElse(null);
        if (empresa == null || !ambito.tieneRol(id, empresa, UserRole.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso para gestionar usuarios");
        }

        if (ambito.esAdminDePlataforma(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar un SUPER_ADMIN");
        }

        // Un usuario recién construido y aún sin persistir no tiene membresías; su
        // empresa es la que se le está asignando, y de eso responde quien llama.
        if (targetUser.getId() != null && !ambito.perteneceALaEmpresa(targetUser.getId(), empresa)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar usuarios de otra empresa");
        }
    }

    /**
     * Garantiza que una sucursal pertenezca a la empresa indicada.
     * Regla multitenant: un usuario nunca puede asignarse/operar una sucursal de otra empresa.
     */
    public void assertBranchInCompany(Branch branch, Long companyId) {
        if (branch == null || companyId == null) return;
        if (branch.getCompany() == null || !branch.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La sucursal no pertenece a la empresa indicada");
        }
    }

    public void assertCanAccessPatient(Long patientId) {
        // TODO(bloque-D): la resuelve authz_patient_read_path, con sus seis vias.
    }

    public void assertCanAccessConversation(Long conversationId) {
        // TODO(bloque-D): idem, por el contacto de la conversacion.
    }

    public void assertCanAccessAppointment(Long appointmentId) {
        // TODO(bloque-D): idem, por el paciente de la cita.
    }
}
