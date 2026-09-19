package com.lostfound.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;












public final class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordUtil() {
        
    }

    
    public static String hash(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hash = hashWithSalt(plainPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
    }

    




    public static boolean verify(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null || !storedValue.contains("$")) {
            return false;
        }
        String[] parts = storedValue.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = hashWithSalt(plainPassword, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            
            
            throw new IllegalStateException("SHA-256 algorithm not available on this JVM", e);
        }
    }
}
