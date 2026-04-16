package com.iclinic.iclinicbackend.shared.exception;

/**
 * Excepción para cuando una empresa no es encontrada.
 */
public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(Long id) {
        super("Empresa con ID " + id + " no encontrada");
    }

    public CompanyNotFoundException(String message) {
        super(message);
    }
}

