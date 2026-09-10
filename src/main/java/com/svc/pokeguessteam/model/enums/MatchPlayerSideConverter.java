package com.svc.pokeguessteam.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MatchPlayerSideConverter implements AttributeConverter<MatchPlayerSide, String> {

    @Override
    public String convertToDatabaseColumn(MatchPlayerSide attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public MatchPlayerSide convertToEntityAttribute(String dbData) {
        return MatchPlayerSide.fromLegacy(dbData);
    }
}
