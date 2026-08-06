package com.corvian.payroll_payment_orchestrator.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private static volatile CryptoService cryptoService;

    @Autowired
    public void setCryptoService(CryptoService service) {
        EncryptedStringConverter.cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return requireCryptoService().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return requireCryptoService().decrypt(dbData);
    }

    private CryptoService requireCryptoService() {
        CryptoService service = cryptoService;
        if (service == null) {
            throw new IllegalStateException("CryptoService is not initialized; refusing to persist or expose plaintext data");
        }
        return service;
    }
}
