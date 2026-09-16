package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.service.CryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class CryptoConverter implements AttributeConverter<String, String> {

    private static CryptoService cryptoService;

    @Autowired
    public void setCryptoService(CryptoService service) {
        CryptoConverter.cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || cryptoService == null) {
            return attribute;
        }
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || cryptoService == null) {
            return dbData;
        }
        return cryptoService.decrypt(dbData);
    }
}