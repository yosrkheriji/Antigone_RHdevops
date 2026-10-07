package com.antigone.rh.converter;

import com.antigone.rh.dto.TrancheIrppDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class TranchesIrppConverter implements AttributeConverter<List<TrancheIrppDTO>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<TrancheIrppDTO> attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de sérialisation du barème IRPP", e);
        }
    }

    @Override
    public List<TrancheIrppDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<TrancheIrppDTO>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de désérialisation du barème IRPP", e);
        }
    }
}
