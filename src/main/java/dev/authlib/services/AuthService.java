package dev.authlib.services;

import com.google.gson.Gson;
import dev.authlib.config.Config;
import dev.authlib.models.TokenBlacklist;
import dev.authlib.models.User;
import dev.authlib.utils.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication service
 */

public class AuthService {
    private final Config config;
    private final UserService userService;
    private final JWTHandler jwtHandler;
    private final PasswordHandler passwordHandler;
    private final SessionFactory sessionFactory;
    private final Gson gson = new Gson();

    /**
     * Register request data structure
     */
    public static class RegisterRequest {
        public String email;
        public String password;
        public String firstName;
        public String lastName;

        public RegisterRequest(String email, String password, String firstName, String lastName) {
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    /**
     * Login request data structure
     */
    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    /**
     * Auth response data structure
     */
    public static class AuthResponse {
        public boolean success;
        public Map<String, Object> user;
        public String accessToken;
        public String refreshToken;

        public AuthResponse(boolean success, User user, String accessToken, String refreshToken) {
            this.success = success;
            this.user = userToMap(user);
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        private static Map<String, Object> userToMap(User user) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("isActive", user.getIsActive());
            map.put("isVerified", user.getIsVerified());
            map.put("createdAt", user.getCreatedAt());
            map.put("updatedAt", user.getUpdatedAt());
            map.put("lastLogin", user.getLastLogin());
            return map;
        }
    }

    public AuthService(Config config) {
        this.config = config;
        this.jwtHandler = new JWTHandler(config);
        this.passwordHandler = new PasswordHandler();

        // Initialize Hibernate
        Configuration hibernateConfig = new Configuration();
        hibernateConfig.setProperty("hibernate.connection.url", config.DATABASE_URL);
        hibernateConfig.setProperty("hibernate.connection.username", config.DATABASE_USER);
        hibernateConfig.setProperty("hibernate.connection.password", config.DATABASE_PASSWORD);
        hibernateConfig.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateConfig.setProperty("hibernate.hbm2ddl.auto", "update");
        hibernateConfig.addAnnotatedClass(User.class);
        hibernateConfig.addAnnotatedClass(TokenBlacklist.class);

        this.sessionFactory = hibernateConfig.buildSessionFactory();
        this.userService = new UserService(sessionFactory);
    }

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        EmailValidator.validate(request.email);
        PasswordValidator.validate(request.password);

        User user = userService.createUser(
            request.email,
            request.password,
            request.firstName,
            request.lastName
        );

        Map<String, Object> tokens = generateTokens(user);

        return new AuthResponse(
            true,
            user,
            (String) tokens.get("accessToken"),
            (String) tokens.get("refreshToken")
        );
    }

    /**
     * Login user
     */
    public AuthResponse login(LoginRequest request) {
        EmailValidator.validate(request.email);

        User user = userService.getUserByEmail(request.email);

        if (!user.getIsActive()) {
            throw new InvalidCredentials("User account is deactivated");
        }

        if (!PasswordHandler.verifyPassword(request.password, user.getPasswordHash())) {
            throw new InvalidCredentials("Invalid email or password");
        }

        userService.updateLastLogin(user.getId());

        Map<String, Object> tokens = generateTokens(user);

        return new AuthResponse(
            true,
            user,
            (String) tokens.get("accessToken"),
            (String) tokens.get("refreshToken")
        );
    }

    /**
     * Verify token
     */
    public JWTHandler.TokenPayload verifyToken(String token) {
        return jwtHandler.verifyToken(token);
    }

    /**
     * Refresh access token
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        JWTHandler.TokenPayload decoded = jwtHandler.verifyToken(refreshToken);

        if (!"refresh".equals(decoded.getType())) {
            throw new InvalidToken("Invalid token type");
        }

        if (isTokenBlacklisted(refreshToken)) {
            throw new InvalidToken("Token has been revoked");
        }

        String accessToken = jwtHandler.createAccessToken(decoded.getUserId(), decoded.getEmail(), null);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        return result;
    }

    /**
     * Logout user
     */
    public Map<String, Boolean> logout(String accessToken, String refreshToken) {
        JWTHandler.TokenPayload accessPayload = jwtHandler.decodeToken(accessToken);
        JWTHandler.TokenPayload refreshPayload = jwtHandler.decodeToken(refreshToken);

        blacklistToken(accessToken, accessPayload.getExpiresAt(), accessPayload.getUserId());
        blacklistToken(refreshToken, refreshPayload.getExpiresAt(), refreshPayload.getUserId());

        Map<String, Boolean> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    private Map<String, Object> generateTokens(User user) {
        String accessToken = jwtHandler.createAccessToken(user.getId(), user.getEmail(), null);
        String refreshToken = jwtHandler.createRefreshToken(user.getId(), user.getEmail(), null);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    private void blacklistToken(String token, long expiresAtTimestamp, int userId) {
        try (var session = sessionFactory.openSession()) {
            TokenBlacklist entry = new TokenBlacklist();
            entry.setToken(token);
            entry.setUserId(userId);
            entry.setExpiresAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(expiresAtTimestamp),
                ZoneId.of("UTC")
            ));

            var transaction = session.beginTransaction();
            session.persist(entry);
            transaction.commit();
        } catch (Exception e) {
            throw new DatabaseError("Failed to blacklist token: " + e.getMessage(), e);
        }
    }

    private boolean isTokenBlacklisted(String token) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createQuery(
                "FROM TokenBlacklist WHERE token = :token", TokenBlacklist.class
            );
            query.setParameter("token", token);
            return !query.list().isEmpty();
        } catch (Exception e) {
            throw new DatabaseError("Failed to check token blacklist: " + e.getMessage(), e);
        }
    }
}
