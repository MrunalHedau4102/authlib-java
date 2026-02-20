package dev.authlib.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Password hashing and verification using Argon2
 */

public class PasswordHandler {
    private static final Argon2 argon2 = Argon2Factory.create();

    /**
     * Hash a password using Argon2
     */
    public static String hashPassword(String password) {
        try {
            return argon2.hash(2, 65536, 1, password);
        } catch (Exception e) {
            throw new AuthException("Password hashing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify a password against a hash
     */
    public static boolean verifyPassword(String password, String hash) {
        try {
            return argon2.verify(hash, password);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a password needs rehashing
     */
    public static boolean needsRehashing(String hash) {
        return hash == null || hash.length() < 10;
    }
}
