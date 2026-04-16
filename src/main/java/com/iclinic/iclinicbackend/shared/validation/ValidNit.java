package com.iclinic.iclinicbackend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validador para NIT de Colombia (9-10 dígitos).
 * Un NIT válido debe tener entre 9 y 10 dígitos numéricos.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ColombianNitValidator.class)
@Documented
public @interface ValidNit {
    String message() default "NIT inválido. Debe contener entre 9 y 10 dígitos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

