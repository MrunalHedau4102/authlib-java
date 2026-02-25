package dev.authlib.utils;

public class InvalidToken extends AuthException {
    public InvalidToken(String message) {
        super(message);
    }
}
