package com.iclinic.iclinicbackend.shared.exception;

public class LastSuperAdminException extends RuntimeException {
    public LastSuperAdminException() {
        super("No se puede desactivar el último SUPER_ADMIN activo del sistema");
    }

    public LastSuperAdminException(String message) {
        super(message);
    }
}

