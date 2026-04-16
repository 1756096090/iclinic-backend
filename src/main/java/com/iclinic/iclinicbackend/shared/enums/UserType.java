package com.iclinic.iclinicbackend.shared.enums;

public enum UserType {
    ECUADORIAN("Ecuador"),
    COLOMBIAN("Colombia"),
    PERUVIAN("Perú"),
    INTERNATIONAL("Internacional");

    private final String country;

    UserType(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }
}
