package com.iclinic.iclinicbackend.shared.enums;

public enum UserRole {
    SUPER_ADMIN("Super Administrador"),
    ADMIN("Administrador"),
    DENTIST("Odontólogo"),
    ASSISTANT("Asistente"),
    RECEPTIONIST("Recepcionista"),
    EXTERNAL_DOCTOR("Doctor Externo"),
    PATIENT("Paciente");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


