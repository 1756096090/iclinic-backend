package com.iclinic.iclinicbackend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EcuadorianRucValidator implements ConstraintValidator<ValidRuc, String> {

    private static final int RUC_LENGTH = 13;
    private static final String DIGIT_PATTERN = "^\\d{%d}$".formatted(RUC_LENGTH);

    @Override
    public void initialize(ValidRuc constraintAnnotation) { }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.trim().matches(DIGIT_PATTERN);
    }
}
