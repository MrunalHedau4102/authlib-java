package dev.authlib.utils;

public class DatabaseError extends AuthException {
    public DatabaseError(String message) {
        super(message);
    }

    public DatabaseError(String message, Throwable cause) {
        super(message, cause);
    }
}
