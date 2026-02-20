package dev.authlib.utils;

import java.util.regex.Pattern;

/**
 * Input validators for email and password
 */

public class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    private static final int MAX_EMAIL_LENGTH = 254;

    public static void validate(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationError("Email must not be empty");
        }

        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationError("Email must not exceed 254 characters");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationError("Invalid email format");
        }
    }
}

class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    public static void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationError("Password must not be empty");
        }

        if (password.length() < MIN_LENGTH) {
            throw new ValidationError("Password must be at least 8 characters long");
        }

        if (password.length() > MAX_LENGTH) {
            throw new ValidationError("Password must not exceed 128 characters");
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecialChar) {
            throw new ValidationError(
                "Password must contain uppercase, lowercase, number, and special character"
            );
        }
    }
}
