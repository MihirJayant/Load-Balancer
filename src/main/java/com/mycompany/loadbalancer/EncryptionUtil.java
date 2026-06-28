package com.mycompany.loadbalancer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final byte[] SECRET_KEY = "SuperSecretKey12".getBytes(); // Keep secure

    public static byte[] encrypt(byte[] data, int length) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data, 0, length);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
