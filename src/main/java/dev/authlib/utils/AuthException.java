package dev.authlib.utils;

/**
 * Custom exceptions for AuthLib
 */

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

class UserNotFound extends AuthException {
    public UserNotFound(String message) {
        super(message);
    }
}

class InvalidCredentials extends AuthException {
    public InvalidCredentials(String message) {
        super(message);
    }
}

class InvalidToken extends AuthException {
    public InvalidToken(String message) {
        super(message);
    }
}

class UserAlreadyExists extends AuthException {
    public UserAlreadyExists(String message) {
        super(message);
    }
}

class ValidationError extends AuthException {
    public ValidationError(String message) {
        super(message);
    }
}

class DatabaseError extends AuthException {
    public DatabaseError(String message) {
        super(message);
    }

    public DatabaseError(String message, Throwable cause) {
        super(message, cause);
    }
}
