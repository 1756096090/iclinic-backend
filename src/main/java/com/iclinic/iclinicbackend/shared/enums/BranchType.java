package com.iclinic.iclinicbackend.shared.enums;

public enum BranchType {
    CLINIC("clínica"),
    HOSPITAL("hospital");

    private final String displayName;

    BranchType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
