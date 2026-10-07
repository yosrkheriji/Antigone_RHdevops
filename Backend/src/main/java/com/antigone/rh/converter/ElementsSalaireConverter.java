package com.antigone.rh.converter;

import com.antigone.rh.dto.ElementSalaireDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class ElementsSalaireConverter implements AttributeConverter<List<ElementSalaireDTO>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ElementSalaireDTO> attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de sérialisation des éléments de salaire", e);
        }
    }

    @Override
    public List<ElementSalaireDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<ElementSalaireDTO>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de désérialisation des éléments de salaire", e);
        }
    }
}
