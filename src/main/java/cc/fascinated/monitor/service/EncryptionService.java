package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    public EncryptionService(MonitorProperties monitorProperties) {
        this.key = new SecretKeySpec(parseSecret(monitorProperties.getEncryptionSecret()), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            this.secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, this.key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt value", ex);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] encrypted = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, this.key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to decrypt value", ex);
        }
    }

    private static byte[] parseSecret(String secret) {
        if (secret == null || secret.isBlank() || "set_me".equals(secret)) {
            throw new IllegalStateException("monitor.encryption-secret must be configured");
        }

        byte[] keyBytes;
        if (secret.length() == KEY_LENGTH_BYTES * 2 && secret.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            keyBytes = HexFormat.of().parseHex(secret);
        } else {
            keyBytes = Base64.getDecoder().decode(secret);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException("monitor.encryption-secret must decode to 32 bytes");
        }
        return keyBytes;
    }
}
