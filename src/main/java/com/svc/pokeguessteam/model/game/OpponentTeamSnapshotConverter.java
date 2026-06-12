package com.svc.pokeguessteam.model.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.dto.game.GameHistoryOpponentSlotDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class OpponentTeamSnapshotConverter implements AttributeConverter<List<GameHistoryOpponentSlotDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<GameHistoryOpponentSlotDto>> TYPE =
            new TypeReference<>() {
            };

    @Override
    public String convertToDatabaseColumn(List<GameHistoryOpponentSlotDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Não foi possível serializar o time adversário.", ex);
        }
    }

    @Override
    public List<GameHistoryOpponentSlotDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Não foi possível ler o time adversário.", ex);
        }
    }
}
