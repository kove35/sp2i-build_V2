package com.sp2i.domain.capex;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * Ce converter transforme une liste Java en chaine SQL, puis l'inverse.
 *
 * Pourquoi ce choix ?
 * - l'utilisateur veut une liste de dependances
 * - PostgreSQL pourrait stocker un tableau ou du JSON
 * - mais pour un projet pedagogique, une chaine "lot1,lot2,lot3"
 *   reste tres simple a comprendre
 *
 * JPA appelle automatiquement ce converter grace a @Convert.
 */
@Converter
public class LotDependencyListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        return attribute.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((first, second) -> first + "," + second)
                .orElse(null);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
