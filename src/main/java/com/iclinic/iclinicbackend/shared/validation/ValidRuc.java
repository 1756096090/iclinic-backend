package com.iclinic.iclinicbackend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validador para RUC de Ecuador (13 dígitos).
 * Un RUC válido debe tener exactamente 13 dígitos numéricos.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EcuadorianRucValidator.class)
@Documented
public @interface ValidRuc {
    String message() default "RUC inválido. Debe contener exactamente 13 dígitos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
