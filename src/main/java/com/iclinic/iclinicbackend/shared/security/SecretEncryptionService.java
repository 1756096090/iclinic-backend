package com.iclinic.iclinicbackend.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simétrico AES-256/GCM para tokens y secretos guardados en BD.
 *
 * <p>Formato del texto cifrado almacenado: Base64(iv[12] + ciphertext + tag[16])</p>
 *
 * <p>Para entornos de desarrollo (H2 seed) se acepta el prefijo {@code {plain}}
 * que bypass el cifrado y devuelve el valor en claro. NUNCA usar en producción.</p>
 *
 * <p>La clave debe definirse en {@code encryption.secret-key} como 32 chars ASCII (256 bits).
 * Ejemplo: {@code encryption.secret-key=iclinic-dev-secret-key-32-chars!!}</p>
 */
@Component
public class SecretEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final String PLAIN_PREFIX = "{plain}";

    private final SecretKey secretKey;

    public SecretEncryptionService(
            @Value("${encryption.secret-key:iclinic-dev-secret-key-32-chars!!}") String rawKey) {
        byte[] keyBytes = rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "encryption.secret-key debe tener al menos 32 caracteres (256 bits)");
        }
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, 32);
        this.secretKey = new SecretKeySpec(key32, "AES");
    }

    /**
     * Cifra el texto plano con AES-256/GCM.
     * Si el texto empieza con {@code {plain}} lo devuelve tal cual (solo para dev).
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        if (plainText.startsWith(PLAIN_PREFIX)) return plainText;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el valor", e);
        }
    }

    /**
     * Descifra un valor cifrado con {@link #encrypt}.
     * Soporta prefijo {@code {plain}} para seeds de desarrollo.
     * Soporta Base64 puro para tokens legacy (retrocompatibilidad temporal).
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;

        // Desarrollo: tokens guardados como {plain}valor_real
        if (cipherText.startsWith(PLAIN_PREFIX)) {
            return cipherText.substring(PLAIN_PREFIX.length());
        }

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // Mínimo = IV(12) + tag(16) = 28 bytes. Menos → legacy Base64
            if (combined.length < IV_LENGTH + 16) {
                return new String(combined, java.nio.charset.StandardCharsets.UTF_8);
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encryptedData = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(encryptedData);

            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el valor", e);
        }
    }
}

