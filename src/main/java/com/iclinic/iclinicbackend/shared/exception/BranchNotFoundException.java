package com.iclinic.iclinicbackend.shared.exception;

/**
 * Excepción para cuando una sucursal no es encontrada.
 */
public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(Long id) {
        super("Sucursal con ID " + id + " no encontrada");
    }

    public BranchNotFoundException(String message) {
        super(message);
    }
}

