package dev.authlib.utils;

import java.util.regex.Pattern;

/**
 * Email validation
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
