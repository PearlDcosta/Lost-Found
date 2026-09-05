package com.lostfound.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted password hashing using only java.security (no external
 * hashing library). This is deliberately kept simple and dependency-free
 * to satisfy the "Core Java only" requirement — a production system
 * would prefer a slow, memory-hard algorithm like bcrypt/Argon2, but
 * those require a third-party library. SHA-256 with a random salt is
 * a reasonable, explainable middle ground for an academic project and
 * is a large step up from storing plain text.
 *
 * Stored format: base64(salt) + "$" + base64(hash)
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordUtil() {
        // static utility class, no instances
    }

    /** Hashes a plain-text password with a freshly generated random salt. */
    public static String hash(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hash = hashWithSalt(plainPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a plain-text password against a previously stored
     * "salt$hash" string. Returns false (never throws) for malformed
     * stored values, so a corrupted row fails safe rather than crashing.
     */
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
            // malformed base64 in a corrupted row
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
            // SHA-256 is guaranteed present on every standard JVM, so this
            // branch should be unreachable — fail loudly if it ever isn't.
            throw new IllegalStateException("SHA-256 algorithm not available on this JVM", e);
        }
    }
}
