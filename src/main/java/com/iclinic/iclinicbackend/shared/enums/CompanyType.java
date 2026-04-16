package com.iclinic.iclinicbackend.shared.enums;

public enum CompanyType {
    ECUADORIAN("ecuatoriana", "RUC"),
    COLOMBIAN("colombiana", "NIT");

    private final String displayName;
    private final String taxIdName;

    CompanyType(String displayName, String taxIdName) {
        this.displayName = displayName;
        this.taxIdName = taxIdName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaxIdName() {
        return taxIdName;
    }
}
