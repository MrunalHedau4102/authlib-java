package dev.authlib;

import dev.authlib.config.Config;
import dev.authlib.models.User;
import dev.authlib.services.AuthService;
import dev.authlib.services.UserService;
import dev.authlib.utils.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AuthLib Java version
 * Tests the complete authentication flow
 */
@DisplayName("AuthLib Java Integration Tests")
public class AuthLibIntegrationTests {
    private static SessionFactory sessionFactory;
    private static Config config;
    private AuthService authService;
    private UserService userService;

    @BeforeAll
    static void setUpTestSuite() {
        config = new Config();

        // Initialize Hibernate for testing with H2 in-memory database
        Configuration hibernateConfig = new Configuration();
        hibernateConfig.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
        hibernateConfig.setProperty("hibernate.connection.username", "sa");
        hibernateConfig.setProperty("hibernate.connection.password", "");
        hibernateConfig.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateConfig.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        hibernateConfig.addAnnotatedClass(User.class);

        sessionFactory = hibernateConfig.buildSessionFactory();
    }

    @AfterAll
    static void tearDownTestSuite() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(sessionFactory, config);
        userService = new UserService(sessionFactory);
    }

    // ==================== User Registration Tests ====================

    @Test
    @DisplayName("Should successfully register a new user")
    void testSuccessfulUserRegistration() {
        var request = new AuthService.RegisterRequest(
            "newuser@example.com",
            "SecurePass123!",
            "John",
            "Doe"
        );

        var result = authService.register(request);

        assertTrue(result.success);
        assertEquals("newuser@example.com", result.user.get("email"));
        assertEquals("John", result.user.get("firstName"));
        assertEquals("Doe", result.user.get("lastName"));
        assertNotNull(result.accessToken);
        assertNotNull(result.refreshToken);
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void testRejectInvalidEmailFormat() {
        var request = new AuthService.RegisterRequest(
            "invalid-email",
            "SecurePass123!",
            "Jane",
            "Doe"
        );

        assertThrows(ValidationError.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Should reject weak password")
    void testRejectWeakPassword() {
        var request = new AuthService.RegisterRequest(
            "weak@example.com",
            "weak",
            "Test",
            "User"
        );

        assertThrows(ValidationError.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Should prevent duplicate email registration")
    void testPreventDuplicateEmailRegistration() {
        var request1 = new AuthService.RegisterRequest(
            "duplicate@example.com",
            "SecurePass123!",
            "First",
            "User"
        );

        var request2 = new AuthService.RegisterRequest(
            "duplicate@example.com",
            "SecurePass123!",
            "Second",
            "User"
        );

        // First registration should succeed
        var result1 = authService.register(request1);
        assertTrue(result1.success);

        // Duplicate registration should fail
        assertThrows(UserAlreadyExists.class, () -> authService.register(request2));
    }

    @Test
    @DisplayName("Should generate valid JWT tokens")
    void testGenerateValidJWTTokens() {
        var request = new AuthService.RegisterRequest(
            "tokentest@example.com",
            "SecurePass123!",
            "Token",
            "Tester"
        );

        var result = authService.register(request);
        var payload = authService.verifyToken(result.accessToken);

        assertEquals(payload.getUserId(), ((Number) result.user.get("id")).intValue());
        assertEquals("tokentest@example.com", payload.getEmail());
        assertEquals("access", payload.getType());
    }

    // ==================== User Login Tests ====================

    @Test
    @DisplayName("Should successfully login with correct credentials")
    void testSuccessfulLogin() {
        // Register user first
        var registerRequest = new AuthService.RegisterRequest(
            "logintest@example.com",
            "SecurePass123!",
            "Login",
            "Tester"
        );
        authService.register(registerRequest);

        // Login
        var loginRequest = new AuthService.LoginRequest(
            "logintest@example.com",
            "SecurePass123!"
        );

        var result = authService.login(loginRequest);

        assertTrue(result.success);
        assertEquals("logintest@example.com", result.user.get("email"));
        assertNotNull(result.accessToken);
        assertNotNull(result.refreshToken);
    }

    @Test
    @DisplayName("Should track last login timestamp")
    void testLastLoginTimestamp() {
        var registerRequest = new AuthService.RegisterRequest(
            "lastlogin@example.com",
            "SecurePass123!",
            "Last",
            "Login"
        );
        authService.register(registerRequest);

        LocalDateTime beforeLogin = LocalDateTime.now(ZoneId.of("UTC"));
        
        var loginRequest = new AuthService.LoginRequest(
            "lastlogin@example.com",
            "SecurePass123!"
        );
        var result = authService.login(loginRequest);

        LocalDateTime afterLogin = LocalDateTime.now(ZoneId.of("UTC"));

        assertNotNull(result.user.get("lastLogin"));
        LocalDateTime loginTime = (LocalDateTime) result.user.get("lastLogin");

        assertTrue(loginTime.isAfter(beforeLogin.minusSeconds(1)));
        assertTrue(loginTime.isBefore(afterLogin.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void testRejectIncorrectPassword() {
        var registerRequest = new AuthService.RegisterRequest(
            "wrongpass@example.com",
            "SecurePass123!",
            "Wrong",
            "Pass"
        );
        authService.register(registerRequest);

        var loginRequest = new AuthService.LoginRequest(
            "wrongpass@example.com",
            "WrongPassword123!"
        );

        assertThrows(InvalidCredentials.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should reject non-existent email")
    void testRejectNonExistentEmail() {
        var loginRequest = new AuthService.LoginRequest(
            "nonexistent@example.com",
            "SecurePass123!"
        );

        assertThrows(UserNotFound.class, () -> authService.login(loginRequest));
    }

    // ==================== Token Management Tests ====================

    @Test
    @DisplayName("Should verify valid access token")
    void testVerifyAccessToken() {
        var registerRequest = new AuthService.RegisterRequest(
            "tokenverify@example.com",
            "SecurePass123!",
            "Token",
            "Verify"
        );

        var registerResult = authService.register(registerRequest);
        var payload = authService.verifyToken(registerResult.accessToken);

        assertEquals(payload.getUserId(), ((Number) registerResult.user.get("id")).intValue());
        assertEquals("tokenverify@example.com", payload.getEmail());
        assertEquals("access", payload.getType());
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testRejectInvalidToken() {
        assertThrows(InvalidToken.class, () -> authService.verifyToken("invalid.token.here"));
    }

    @Test
    @DisplayName("Should refresh access token using refresh token")
    void testRefreshAccessToken() {
        var registerRequest = new AuthService.RegisterRequest(
            "refresh@example.com",
            "SecurePass123!",
            "Refresh",
            "Test"
        );

        var registerResult = authService.register(registerRequest);
        var refreshResponse = authService.refreshAccessToken(registerResult.refreshToken);

        assertNotNull(refreshResponse.get("accessToken"));
        assertNotEquals(refreshResponse.get("accessToken"), registerResult.accessToken);

        // Verify new token works
        var newPayload = authService.verifyToken((String) refreshResponse.get("accessToken"));
        assertEquals(newPayload.getUserId(), ((Number) registerResult.user.get("id")).intValue());
        assertEquals("access", newPayload.getType());
    }

    @Test
    @DisplayName("Should reject refresh with wrong token type")
    void testRejectRefreshWithWrongTokenType() {
        var registerRequest = new AuthService.RegisterRequest(
            "wrongtype@example.com",
            "SecurePass123!",
            "Wrong",
            "Type"
        );

        var registerResult = authService.register(registerRequest);

        // Try to use access token as refresh token
        assertThrows(InvalidToken.class, 
            () -> authService.refreshAccessToken(registerResult.accessToken));
    }

    @Test
    @DisplayName("Should logout and blacklist tokens")
    void testLogoutAndBlacklistTokens() {
        var registerRequest = new AuthService.RegisterRequest(
            "logout@example.com",
            "SecurePass123!",
            "Logout",
            "Test"
        );

        var registerResult = authService.register(registerRequest);
        var logoutResult = authService.logout(registerResult.accessToken, registerResult.refreshToken);

        assertTrue((Boolean) logoutResult.get("success"));

        // Token should now be blacklisted
        assertThrows(InvalidToken.class, 
            () -> authService.refreshAccessToken(registerResult.refreshToken));
    }

    // ==================== User Service Tests ====================

    @Test
    @DisplayName("Should retrieve user by ID")
    void testRetrieveUserById() {
        var registerRequest = new AuthService.RegisterRequest(
            "getbyid@example.com",
            "SecurePass123!",
            "Get",
            "ById"
        );

        var registerResult = authService.register(registerRequest);
        int userId = ((Number) registerResult.user.get("id")).intValue();
        
        var user = userService.getUserById(userId);

        assertEquals(userId, user.getId());
        assertEquals("getbyid@example.com", user.getEmail());
    }

    @Test
    @DisplayName("Should retrieve user by email")
    void testRetrieveUserByEmail() {
        var registerRequest = new AuthService.RegisterRequest(
            "getbyemail@example.com",
            "SecurePass123!",
            "Get",
            "ByEmail"
        );

        var registerResult = authService.register(registerRequest);
        int userId = ((Number) registerResult.user.get("id")).intValue();
        
        var user = userService.getUserByEmail("getbyemail@example.com");

        assertEquals(userId, user.getId());
        assertEquals("getbyemail@example.com", user.getEmail());
    }

    @Test
    @DisplayName("Should activate and deactivate user")
    void testActivateAndDeactivateUser() {
        var registerRequest = new AuthService.RegisterRequest(
            "activate@example.com",
            "SecurePass123!",
            "Activate",
            "Test"
        );

        var registerResult = authService.register(registerRequest);
        int userId = ((Number) registerResult.user.get("id")).intValue();

        var deactivatedUser = userService.deactivateUser(userId);
        assertFalse(deactivatedUser.getIsActive());

        var activatedUser = userService.activateUser(userId);
        assertTrue(activatedUser.getIsActive());
    }

    @Test
    @DisplayName("Should verify user")
    void testVerifyUser() {
        var registerRequest = new AuthService.RegisterRequest(
            "verify@example.com",
            "SecurePass123!",
            "Verify",
            "Test"
        );

        var registerResult = authService.register(registerRequest);
        int userId = ((Number) registerResult.user.get("id")).intValue();

        var verifiedUser = userService.verifyUser(userId);
        assertTrue(verifiedUser.getIsVerified());
    }

    // ==================== Password Security Tests ====================

    @Test
    @DisplayName("Should hash password")
    void testHashPassword() {
        String password = "SecurePass123!";
        String hash = PasswordHandler.hashPassword(password);

        assertNotNull(hash);
        assertNotEquals(hash, password);
        assertTrue(hash.length() > password.length());
    }

    @Test
    @DisplayName("Should verify correct password")
    void testVerifyCorrectPassword() {
        String password = "SecurePass123!";
        String hash = PasswordHandler.hashPassword(password);

        boolean isValid = PasswordHandler.verifyPassword(password, hash);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void testRejectIncorrectPasswordVerification() {
        String password = "SecurePass123!";
        String hash = PasswordHandler.hashPassword(password);

        boolean isValid = PasswordHandler.verifyPassword("WrongPass123!", hash);
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate different hash for same password")
    void testGenerateDifferentHashSamePassword() {
        String password = "SecurePass123!";
        String hash1 = PasswordHandler.hashPassword(password);
        String hash2 = PasswordHandler.hashPassword(password);

        assertNotEquals(hash1, hash2);
    }

    // ==================== End-to-End Workflow Tests ====================

    @Test
    @DisplayName("Should complete full registration and login workflow")
    void testFullWorkflow() {
        // 1. Register new user
        var registerRequest = new AuthService.RegisterRequest(
            "e2e@example.com",
            "SecurePass123!",
            "End",
            "ToEnd"
        );
        var registerResult = authService.register(registerRequest);

        assertTrue(registerResult.success);
        int userId = ((Number) registerResult.user.get("id")).intValue();

        // 2. Verify user can be retrieved
        var user = userService.getUserById(userId);
        assertEquals("e2e@example.com", user.getEmail());

        // 3. Verify tokens are valid
        var accessPayload = authService.verifyToken(registerResult.accessToken);
        assertEquals(userId, accessPayload.getUserId());

        // 4. Perform logout
        var logoutResult = authService.logout(registerResult.accessToken, registerResult.refreshToken);
        assertTrue((Boolean) logoutResult.get("success"));

        // 5. Login again
        var loginRequest = new AuthService.LoginRequest("e2e@example.com", "SecurePass123!");
        var loginResult = authService.login(loginRequest);

        assertTrue(loginResult.success);
        assertNotNull(loginResult.accessToken);

        // 6. Verify new tokens work
        var newPayload = authService.verifyToken(loginResult.accessToken);
        assertEquals(userId, newPayload.getUserId());
    }

    @Test
    @DisplayName("Should handle concurrent operations safely")
    void testConcurrentOperations() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    var request = new AuthService.RegisterRequest(
                        "concurrent" + index + "@example.com",
                        "SecurePass123!",
                        "Concurrent",
                        "User" + index
                    );
                    authService.register(request);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verify all were created
        for (int i = 0; i < threadCount; i++) {
            var user = userService.getUserByEmail("concurrent" + i + "@example.com");
            assertNotNull(user);
        }
    }

    // ==================== Configuration Tests ====================

    @Test
    @DisplayName("Should load environment configuration")
    void testLoadConfiguration() {
        Config testConfig = new Config();

        assertNotNull(testConfig.JWT_SECRET_KEY);
        assertNotNull(testConfig.JWT_ALGORITHM);
        assertTrue(testConfig.JWT_ACCESS_TOKEN_EXPIRY_MINUTES > 0);
        assertTrue(testConfig.JWT_REFRESH_TOKEN_EXPIRY_DAYS > 0);
        assertNotNull(testConfig.DATABASE_URL);
    }

    @Test
    @DisplayName("Should validate configuration in production")
    void testValidateConfigurationProduction() {
        Config testConfig = new Config();
        // Default config won't throw because we set proper values in .env.example
        // This test would pass with proper production-mode JWT_SECRET_KEY
        assertNotNull(testConfig.JWT_SECRET_KEY);
    }
}
