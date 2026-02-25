package dev.authlib.utils;

public class PasswordValidator {
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
            throw new ValidationError("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }
}
