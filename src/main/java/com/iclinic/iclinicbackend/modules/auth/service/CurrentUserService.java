package com.iclinic.iclinicbackend.modules.auth.service;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        if (auth.getDetails() instanceof User user) {
            return user;
        }
        String uid = (String) auth.getPrincipal();
        return userRepository.findByExternalAuthId(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    public boolean isSuperAdmin() {
        return getCurrentUser().getRole() == UserRole.SUPER_ADMIN;
    }

    public boolean isCompanyAdmin() {
        UserRole role = getCurrentUser().getRole();
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN;
    }

    public void assertCanAccessCompany(Long companyId) {
        User user = getCurrentUser();
        if (user.getRole() == UserRole.SUPER_ADMIN) return;
        if (user.getCompany() == null || !user.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta empresa");
        }
    }

    public void assertCanAccessBranch(Long branchId) {
        User user = getCurrentUser();
        if (user.getRole() == UserRole.SUPER_ADMIN) return;
        if (user.getBranch() != null && !user.getBranch().getId().equals(branchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta sucursal");
        }
    }

    public void assertCanManageUser(User targetUser) {
        User current = getCurrentUser();
        if (current.getRole() == UserRole.SUPER_ADMIN) return;
        if (current.getRole() == UserRole.ADMIN) {
            if (targetUser.getRole() == UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar un SUPER_ADMIN");
            }
            if (targetUser.getCompany() != null && current.getCompany() != null
                    && !targetUser.getCompany().getId().equals(current.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar usuarios de otra empresa");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso para gestionar usuarios");
    }

    public void assertSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo SUPER_ADMIN puede ejecutar esta acción");
        }
    }

    public boolean canAccessCompany(Long companyId) {
        User user = getCurrentUser();
        if (user.getRole() == UserRole.SUPER_ADMIN) return true;
        return user.getCompany() != null && user.getCompany().getId().equals(companyId);
    }

    public boolean canAccessBranch(Long branchId) {
        User user = getCurrentUser();
        if (user.getRole() == UserRole.SUPER_ADMIN) return true;
        return user.getBranch() != null && user.getBranch().getId().equals(branchId);
    }

    /**
     * Empresa del usuario autenticado. Null para SUPER_ADMIN global (sin empresa).
     */
    public Long getCurrentCompanyId() {
        Company company = getCurrentUser().getCompany();
        return company != null ? company.getId() : null;
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
        // TODO: implementar validación multitenant por companyId/branchId/asignación
    }

    public void assertCanAccessConversation(Long conversationId) {
        // TODO: implementar validación multitenant por companyId/branchId
    }

    public void assertCanAccessAppointment(Long appointmentId) {
        // TODO: implementar validación multitenant por companyId/branchId
    }
}
