package dev.authlib.utils;

public class InvalidCredentials extends AuthException {
    public InvalidCredentials(String message) {
        super(message);
    }
}
