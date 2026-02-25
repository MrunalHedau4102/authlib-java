package dev.authlib.utils;

public class UserAlreadyExists extends AuthException {
    public UserAlreadyExists(String message) {
        super(message);
    }
}
