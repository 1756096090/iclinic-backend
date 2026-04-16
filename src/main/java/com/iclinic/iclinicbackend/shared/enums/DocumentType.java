package com.iclinic.iclinicbackend.shared.enums;

public enum DocumentType {
    CEDULA_EC("Cédula de identidad", "Ecuador"),
    RUC_EC("RUC", "Ecuador"),
    CEDULA_CO("Cédula de ciudadanía", "Colombia"),
    NIT_CO("NIT", "Colombia"),
    DNI_PE("DNI", "Perú"),
    RUC_PE("RUC", "Perú"),
    PASSPORT("Pasaporte", "Internacional");

    private final String displayName;
    private final String country;

    DocumentType(String displayName, String country) {
        this.displayName = displayName;
        this.country = country;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountry() {
        return country;
    }
}
