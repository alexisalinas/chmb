package com.banamex.cops.update_offer.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aplica las reglas de los esquemas de BW (obligatorio / opcional / lista) que llevan los DTOs.
 * Se usa donde el código ARMA un DTO (RqHeader, Start de supresión, Start de zona horaria).
 */
@Component
public class DtoValidator {

    private final Validator validator;

    @Autowired
    public DtoValidator(ObjectProvider<Validator> provider) {
        Validator v = provider.getIfAvailable();
        this.validator = v != null ? v : Validation.buildDefaultValidatorFactory().getValidator();
    }

    public DtoValidator(Validator validator) {
        this.validator = validator;
    }

    /** Lista "ruta: mensaje" de cada regla incumplida, ordenada. Vacía si todo está bien. */
    public List<String> violations(Object dto) {
        return validator.validate(dto).stream()
                .map((ConstraintViolation<Object> v) -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
    }

    /** Lanza IllegalArgumentException con el detalle si el DTO incumple alguna regla. */
    public void validateOrThrow(Object dto) {
        List<String> v = violations(dto);
        if (!v.isEmpty()) {
            throw new IllegalArgumentException(
                    "Datos inválidos en " + dto.getClass().getSimpleName() + ": " + String.join("; ", v));
        }
    }
}
