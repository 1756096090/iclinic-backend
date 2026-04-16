package com.iclinic.iclinicbackend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ColombianNitValidator implements ConstraintValidator<ValidNit, String> {

    private static final int MIN_LENGTH = 9;
    private static final int MAX_LENGTH = 10;
    private static final String DIGIT_PATTERN = "^\\d{%d,%d}$".formatted(MIN_LENGTH, MAX_LENGTH);

    @Override
    public void initialize(ValidNit constraintAnnotation) { }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.trim().matches(DIGIT_PATTERN);
    }
}
