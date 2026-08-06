package com.corvian.payroll_payment_orchestrator.shared.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class CryptoService {
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String CURRENT_CIPHERTEXT_VERSION = "v2:";
    private static final String PREVIOUS_CIPHERTEXT_VERSION = "v1:";
    private static final byte[] AAD = "payroll-payment-orchestrator:v2".getBytes(StandardCharsets.UTF_8);
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH = 128;

    private final byte[] currentEncryptionKey;
    private final byte[] previousEncryptionKey;
    private final byte[] legacyEncryptionKey;
    private final byte[] previousLegacyEncryptionKey;
    private final byte[] hashKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CryptoService(
            @Value("${app.crypto.encryption-key}") String encryptionKey,
            @Value("${app.crypto.previous-encryption-key:}") String previousEncryptionKey,
            @Value("${app.crypto.hash-key}") String hashKey
    ) {
        byte[] sourceKey = encryptionKey.getBytes(StandardCharsets.UTF_8);
        this.currentEncryptionKey = sha256(sourceKey);
        this.legacyEncryptionKey = Arrays.copyOf(sourceKey, 32);
        if (previousEncryptionKey == null || previousEncryptionKey.isBlank()) {
            this.previousEncryptionKey = null;
            this.previousLegacyEncryptionKey = null;
        } else {
            byte[] previousSourceKey = previousEncryptionKey.getBytes(StandardCharsets.UTF_8);
            this.previousEncryptionKey = sha256(previousSourceKey);
            this.previousLegacyEncryptionKey = Arrays.copyOf(previousSourceKey, 32);
        }
        this.hashKey = hashKey.getBytes(StandardCharsets.UTF_8);
    }

    public CryptoService(String encryptionKey, String hashKey) {
        this(encryptionKey, "", hashKey);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        return CURRENT_CIPHERTEXT_VERSION + encryptPayload(plaintext, currentEncryptionKey, AAD);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            if (ciphertext.startsWith(CURRENT_CIPHERTEXT_VERSION)) {
                String payload = ciphertext.substring(CURRENT_CIPHERTEXT_VERSION.length());
                return decryptWithFallback(payload, currentEncryptionKey, previousEncryptionKey, AAD);
            }
            if (ciphertext.startsWith(PREVIOUS_CIPHERTEXT_VERSION)) {
                String payload = ciphertext.substring(PREVIOUS_CIPHERTEXT_VERSION.length());
                return decryptWithFallback(payload, legacyEncryptionKey, previousLegacyEncryptionKey, null);
            }
            return decryptWithFallback(ciphertext, legacyEncryptionKey, previousLegacyEncryptionKey, null);
        } catch (Exception ex) {
            throw new IllegalStateException("Decryption failed", ex);
        }
    }

    public String hmacSha256(String value) {
        if (value == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Hashing failed", ex);
        }
    }

    public boolean constantTimeHashMatches(String value, String expectedHash) {
        String calculated = hmacSha256(value);
        return calculated != null && expectedHash != null
                && MessageDigest.isEqual(calculated.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptWithFallback(String encoded, byte[] primaryKey, byte[] fallbackKey, byte[] aad) throws Exception {
        try {
            return decryptPayload(encoded, primaryKey, aad);
        } catch (Exception primaryFailure) {
            if (fallbackKey == null) throw primaryFailure;
            try {
                return decryptPayload(encoded, fallbackKey, aad);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    private String encryptPayload(String plaintext, byte[] key, byte[] aad) {
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            if (aad != null) cipher.updateAAD(aad);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception ex) {
            throw new IllegalStateException("Encryption failed", ex);
        }
    }

    private String decryptPayload(String encoded, byte[] key, byte[] aad) throws Exception {
        byte[] input = Base64.getDecoder().decode(encoded);
        if (input.length <= IV_SIZE) throw new IllegalArgumentException("Ciphertext is too short");
        byte[] iv = Arrays.copyOfRange(input, 0, IV_SIZE);
        byte[] encrypted = Arrays.copyOfRange(input, IV_SIZE, input.length);
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
        if (aad != null) cipher.updateAAD(aad);
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive encryption key", ex);
        }
    }
}
