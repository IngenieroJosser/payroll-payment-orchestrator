package com.corvian.payroll_payment_orchestrator.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private static CryptoService cryptoService;

    @Autowired
    public void setCryptoService(CryptoService cryptoService) {
        EncryptedStringConverter.cryptoService = cryptoService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || cryptoService == null) return attribute;
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || cryptoService == null) return dbData;
        return cryptoService.decrypt(dbData);
    }
}
