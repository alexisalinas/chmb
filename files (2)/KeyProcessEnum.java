package com.banamex.cops.update_offer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum que representa las versiones de ejecución permitidas para el keyProcess.
 * Basado en el esquema: Enum: [ V1, V2, V3 ]
 */
public enum KeyProcessEnum {
    V1("V1"),
    V2("V2"),
    V3("V3");

    private final String value;

    KeyProcessEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KeyProcessEnum fromValue(String value) {
        for (KeyProcessEnum process : KeyProcessEnum.values()) {
            if (process.value.equalsIgnoreCase(value)) {
                return process;
            }
        }
        throw new IllegalArgumentException("Valor inválido para KeyProcessEnum: " + value);
    }
}