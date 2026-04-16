package com.iclinic.iclinicbackend.shared.enums;

public enum UserRole {
    ADMIN("Administrador"),
    DENTIST("Odontólogo"),
    ASSISTANT("Asistente"),
    RECEPTIONIST("Recepcionista"),
    PATIENT("Paciente");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


