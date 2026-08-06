package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoServiceTest {
    private static final String ENCRYPTION_SECRET = "test-encryption-secret-with-more-than-32-bytes";

    @Test
    void should_encrypt_with_versioned_aes_gcm_and_round_trip() {
        CryptoService service = new CryptoService(ENCRYPTION_SECRET, "test-hmac-secret-with-more-than-thirty-two-bytes");

        String ciphertext = service.encrypt("1234567890");

        assertThat(ciphertext).startsWith("v2:");
        assertThat(ciphertext).doesNotContain("1234567890");
        assertThat(service.decrypt(ciphertext)).isEqualTo("1234567890");
    }


    @Test
    void should_decrypt_v2_ciphertext_with_previous_key_during_rotation() {
        CryptoService oldService = new CryptoService(ENCRYPTION_SECRET,
                "test-hmac-secret-with-more-than-thirty-two-bytes");
        String ciphertext = oldService.encrypt("rotating-account");

        CryptoService rotatedService = new CryptoService(
                "new-encryption-secret-with-more-than-thirty-two-bytes",
                ENCRYPTION_SECRET,
                "test-hmac-secret-with-more-than-thirty-two-bytes");

        assertThat(rotatedService.decrypt(ciphertext)).isEqualTo("rotating-account");
        assertThat(rotatedService.encrypt("new-value")).startsWith("v2:");
    }

    @Test
    void should_read_legacy_unversioned_ciphertext_during_key_migration() throws Exception {
        CryptoService service = new CryptoService(ENCRYPTION_SECRET, "test-hmac-secret-with-more-than-thirty-two-bytes");
        String legacy = legacyEncrypt("legacy-account", ENCRYPTION_SECRET);

        assertThat(service.decrypt(legacy)).isEqualTo("legacy-account");
    }

    private static String legacyEncrypt(String plaintext, String keyText) throws Exception {
        byte[] key = Arrays.copyOf(keyText.getBytes(StandardCharsets.UTF_8), 32);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] output = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, output, 0, iv.length);
        System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(output);
    }
}
