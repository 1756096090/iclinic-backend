package com.iclinic.iclinicbackend.shared.exception;

/**
 * Excepción para cuando se intenta crear una empresa con RUC/NIT duplicado.
 */
public class DuplicateCompanyException extends RuntimeException {

    public DuplicateCompanyException(String message) {
        super(message);
    }

    public DuplicateCompanyException(String type, String value) {
        super("Ya existe una empresa " + type + " con el identificador: " + value);
    }
}

