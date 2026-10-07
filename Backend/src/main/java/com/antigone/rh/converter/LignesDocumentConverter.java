package com.antigone.rh.converter;

import com.antigone.rh.dto.LigneDocumentDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class LignesDocumentConverter implements AttributeConverter<List<LigneDocumentDTO>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<LigneDocumentDTO> attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de sérialisation des lignes du document", e);
        }
    }

    @Override
    public List<LigneDocumentDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<LigneDocumentDTO>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de désérialisation des lignes du document", e);
        }
    }
}
