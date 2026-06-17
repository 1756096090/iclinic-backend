package com.iclinic.iclinicbackend.shared.exception;

public class UnauthorizedSuperAdminOperationException extends RuntimeException {
    public UnauthorizedSuperAdminOperationException(String message) {
        super(message);
    }
}

